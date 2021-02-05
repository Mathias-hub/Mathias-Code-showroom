%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Simple function to illustrate the frequency spectrum of a signal
%   INPUTS:
%   sig     =     The decired signal
%   fs      =     The sampling frequency
%
%   OUTPUTS:
%   mFreq   =     Maximum Frequency
%
%   Author: Mathias P. Bonnesen, s113918, Technical University of Denmark. 
%   
%   Date: 12/23 - 2016
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [mFreq] = PlotFspec(signal,fs,O)

% Fourier Transform:
x = fftshift(fft(signal));

% Frequency specifications:
N = length(signal);
dF = fs/N;                      % Hz
f = -fs/2:dF:fs/2-dF;           % Hz

% Time axis
t = 0:1/fs:length(signal)/fs-1/fs;

faxis = f(floor(N/2):floor(N*3/4));       % Frequency axis
aaxis = abs(x(floor(N/2):floor(N*3/4)))/N;  % Amplitude axis

% Plotting the spectrum
if strcmp(O,'ON')
    figure(2)
    subplot(2,1,1)
    plot(faxis,aaxis)
    axis tight
    title(sprintf('Segment %i'),'Fontsize',16,'FontWeight','bold')
    xlabel('Frequency [Hz]','Fontsize',15)
    ylabel('Amplitude [mV]','Fontsize',15)
    subplot(2,1,2)
    plot(t,signal)
    xlabel('Time [Sec]','Fontsize',15)
    ylabel('Amplitude [mV]','Fontsize',15)
    set(gca,'Fontsize',15)
end

%% Find maximum amplitude freq
mFreq = faxis(find(aaxis==max(aaxis)));

end

