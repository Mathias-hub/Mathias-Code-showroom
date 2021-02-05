%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% This function loads the data into variables.
% 
% Input:  folder           = Folder containing the data
%
% Output: X_rs,Y_rs,Z_rs   = Accelerations in x,y,z axis, resampled to take
%                            take into account the sampling freq delay.
%         fs_acc           = accelerometer sampling frequency
%         audio            = downsampled audio signal
%         fs_aud           = audio sampling frequency
%
%   Author: Mathias P. Bonnesen, s113918, Technical University of Denmark. 
%   
%   Date: 12/23 - 2016
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


function [Xr,Yr,Zr,fs_acc,audio,fs_aud,X,Y,Z] = load_data(folder)
current_folder = cd;
cd(folder)

% LOAD AUDIO-data
fid = fopen('audio.pcm','r'); % open stream
speech = fread(fid, inf, 'short', 0, 'l'); % read stream
fclose(fid); % close stream
fs_aud = 16E3; % sampling frequence

% Downsample
ds_f = 4;                         % Downsampling Factor
audio = decimate(speech,ds_f);
fs_aud = fs_aud/ds_f;

% LOAD ACC-data
xFile = 'xFile.txt'; yFile = 'yFile.txt'; zFile = 'zFile.txt';
tFile = 'tFile.txt';
delimiter = ',';

X = dlmread(xFile,delimiter);  % Store in x-vector
Y = dlmread(yFile,delimiter);  % Store in y-vector
Z = dlmread(zFile,delimiter);  % Store in z-vector
T = dlmread(tFile,delimiter);  % Store in T-vecPator

%%% RESAMPLE OF ACC SIGNALS
%%
T_s = T - T(1);                          % Start timestamp vector at 0
T_sn = T_s/10^9;                         % Transform to seconds

tvec_res = 0:1/25:length(speech)/fs_aud'; % Define time axis
fs_acc = 25;                             % Resampled FS

[Xr,tx] = resample(X,T_sn,fs_acc,40,80);
[Yr,ty] = resample(Y,T_sn,fs_acc,40,80);
[Zr,tz] = resample(Z,T_sn,fs_acc,40,80);

cd(current_folder) % Return to main folder
end
