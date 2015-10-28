from math import factorial
import matplotlib
import matplotlib.pyplot as plt
from matplotlib.dates import DateFormatter, MinuteLocator
import numpy as np
import datetime
import sys

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
			elif userNP[x] < (userNP[x-1] - 300):
				userNP[x] = userNP[x] + 360
	return userNP

def make_time_array(userNP):
	x = np.array(userNP)
	return x[0:len(x):15] #THE LAST NUMBER CHANGES WITH TEST SIZE, FOR A FULL TEST IT'D PROBS BE 4	

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

data = np.genfromtxt(sys.argv[1], 
		      dtype=["|S13", int, float, float, float, float, float, float], delimiter=',', 
		      names=['time', 'sec', 'azi', 'pitch', 'roll', 'accx', 'accy', 'accz'])

x = data['sec']
y = data['azi']
acc_z = data['accz']
degree_y = set_zero(np.array(np.degrees(y)))
new_degree_y = filter_noise(degree_y)
s_v_y = np.array(savitzky_golay(new_degree_y, 3, 1))
time = make_time_array(data['time'])

plt.figure(1)
plt.title('Raw Data')
plt.plot(x, y, '-')
ax = plt.gca()
ax.set_xticklabels(time)
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
plt.show()

plt.figure(5)
plt.title('Acceleration on Z axis')
plt.plot(x, acc_z, '-')
ax = plt.gca()
ax.set_xticklabels(time)
plt.ylabel('Degrees')
plt.xticks(rotation=70)
plt.xlabel('Time')
plt.show()
