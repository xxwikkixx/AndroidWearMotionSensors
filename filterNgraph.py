from math import factorial
import matplotlib
import matplotlib.pyplot as plt
import numpy as np
import sys
import os

PATH_NAME = os.getcwd()
file_name = sys.argv[1]

#Sets turns to be degrees away from origin instead of degrees away from north
def set_zero(userNP):
	userNP = list(userNP)
	i = userNP[0];
	for x in range(0,len(userNP)):
		userNP[x] = userNP[x]-i;
		if userNP[x] > 180:
			userNP[x]-=360
		if userNP[x] < -180:
			userNP[x]+=360
	return userNP

#Identifies azimuth fluctuations between -180 and 180, then smooths
def filter_noise(userNP):
	userNP = list(userNP)
	for x in range(1, len(userNP)):
		#print str(x) + " " + str(userNP[x])
		if x is None or x-1 is None:
			break
		else:
			if userNP[x] > (userNP[x-1] + 300):
				userNP[x] = userNP[x] - 360
			elif userNP[x] < (userNP[x] - 300):
				userNP[x] = userNP[x] + 360
	return userNP

def make_time_array(userNP):
	x = np.array(userNP)
	return x[0:len(x):len(x)*0.25]
#THE LAST NUMBER CHANGES WITH TEST SIZE, FOR A FULL TEST IT'D PROBS BE 4	

#Savitzky-Golay Filter
#http://wiki.scipy.org/Cookbook/SavitzkyGolay
#https://en.wikipedia.org/w/index.php?title=Savitzky%E2%80%93Golay_filter
def savitzky_golay(y, window_size, order, deriv=0, rate=1):
	try:
		window_size = np.abs(np.int(window_size))
		order = np.abs(np.int(order))
	except ValueError, msg:
		raise ValueError('window_size and order have to be of type int')	
	if window_size % 2 != 1 or window_size < 1:
		raise TypeError('window_size must be a positive odd number')
	if window_size < order + 2:
		raise TypeError('windoe_size is too small for the polynomials order')
	order_range = range(order+1)
	half_window = (window_size - 1) // 2
	#precompute coefficients
	b = np.mat([[k**i for i in order_range] for k in range(-half_window, half_window+1)])
	m = np.linalg.pinv(b).A[deriv] * rate**deriv * factorial(deriv)
	# pad the signal at the extremes with
	# values taken from the signal itself
	firstvals = y[0] - np.abs( y[1:half_window+1][::-1] - y[0] )
	lastvals = y[-1] + np.abs(y[-half_window-1:-1][::-1] - y[-1])
	y = np.concatenate((firstvals, y, lastvals))
	return np.convolve( m[::-1], y, mode='valid')

#CHANGE THE FILE NAME
def write_to_file(arrayOne, arrayTwo):
	combined_array = np.asarray([[arrayOne], [arrayTwo]])
	np.savetxt(file_name+".csv", (arrayOne, arrayTwo), fmt="%s")
		

data = np.genfromtxt(PATH_NAME + file_name, 
		      dtype=["|S13", int, float, float, float, float, float, float], delimiter=',', 
		      names=['time', 'sec', 'azi', 'pitch', 'roll', 'accx', 'accy', 'accz'])

x = data['sec']
y = data['azi']
acc_z = data['accz']
degree_y = set_zero(np.array(np.degrees(y)))
new_degree_y = filter_noise(degree_y)
s_v_y = np.array(savitzky_golay(new_degree_y, 3, 1))
time = make_time_array(data['time'])
np.savetxt(str(PATH_NAME + "_FILTERED_" + file_name), np.transpose([x, s_v_y]), fmt="%s")
other_acc = np.array(np.sqrt(np.square(data['accx']) + np.square(data['accy']) + np.square(data['accz'])))

plt.figure(1)
plt.title('Raw Data')
plt.plot(x, y, '-')
ax = plt.gca()
ax.set_xticklabels(x)
plt.locator_params(nbins=len(time))
plt.ylabel('Radians')
plt.xticks(rotation=70)
plt.xlabel('Time')
plt.show()

plt.figure(2)
plt.title('Adjusted Data')
plt.plot(x, degree_y, '-')
ax = plt.gca()
ax.set_xticklabels(time)
plt.locator_params(nbins=len(time))
plt.ylabel('Degrees')
plt.xticks(rotation=70)
plt.xlabel('Time')
plt.ylim(-400, 400)
plt.show()

plt.figure(3)
plt.title('Filtered Range Data')
plt.plot(x, new_degree_y, '-')
ax = plt.gca()
ax.set_xticklabels(time)
plt.locator_params(nbins=len(time))
plt.ylabel('Degrees')
plt.xticks(rotation=70)
plt.xlabel('Time')
plt.ylim(-400, 400)
plt.show()

plt.figure(4)
plt.title('Savitsky-Golay Filtered Data')
plt.plot(x, s_v_y, '-')
ax = plt.gca()
ax.set_xticklabels(time)
plt.locator_params(nbins=len(time))
plt.ylabel('Degrees')
plt.xticks(rotation=70)
plt.xlabel('Time')
plt.ylim(-400, 400)
plt.show()
'''
plt.figure(5)
plt.title('Acceleration on X axis')
plt.plot(x, data['accx'], '-')
ax = plt.gca()
ax.set_xticklabels(time)
plt.locator_params(nbins=len(time))
plt.ylabel('Meters/second^2')
plt.xticks(rotation=70)
plt.xlabel('Time')
plt.show()

plt.figure(6)
plt.title('Acceleration on Y axis')
plt.plot(x, data['accy'], '-')
ax = plt.gca()
ax.set_xticklabels(time)
plt.locator_params(nbins=len(time))
plt.ylabel('Meters/second^2')
plt.xticks(rotation=70)
plt.xlabel('Time')
plt.show()

plt.figure(7)
plt.title('Acceleration on Z axis')
plt.plot(x, data['accz'], '-')
ax = plt.gca()
ax.set_xticklabels(time)
plt.locator_params(nbins=len(time))
plt.ylabel('Meters/second^2')
plt.xticks(rotation=70)
plt.xlabel('Time')
plt.show()

plt.figure(8)
plt.title('Acceleration on all axes')
plt.plot(x, other_acc, '-')
ax = plt.gca()
ax.set_xticklabels(time)
plt.locator_params(nbins=len(time))
plt.ylabel(' ')
plt.xticks(rotation=70)
plt.xlabel('Time')
plt.show()
'''
