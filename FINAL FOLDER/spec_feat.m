%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Calculates a number of spectral features from an input
% discrete signal.
%
% INPUT:  sig        = Power Spectral Density of signal
%
% OUTPUT: pAvg       = Average Power
%         zcr        = Zero Cross Rate
%         f0         = Spectral peak, low
%         fp         = Spectral peak, high
%         se         = Spectral entropy
%
%   Author: Mathias P. Bonnesen, s113918, Technical University of Denmark. 
%   
%   Date: 12/23 - 2016
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function [ pAvg,fp,se,RW_25,RW_5e,RW_18,RB_1,RB_2,skew,kurt ] = spec_feat(sig)

% Average Power pAvg
pAvg = mean(sig.^2);

% Frequency of the spectral peak with the maximum power frequency
fp = find(sig==max(sig));   % where t is the frequency axis
if length(fp) > 1
    fp = 0;
end

%%% Spectral entronpy %%%
sig_pdf = sig/sum(sig);     % Normalize PSD so it can be viewed as a pdf
se = -sum(sig.*log(sig_pdf));

%%% Ratio of Energy bands
RW_25 = sum(sig(200:500))/sum(sig); % Energy in frq. 200-500 div. by total
RW_5e = sum(sig(500:end))/sum(sig); % Energy in frq. 200-500 div. by total
RW_18 = sum(sig(1:800))/sum(sig);   % Energy in frq. 0-800 div. by total

RB_1 = sum(sig(200:500))/...
    (sum(sig)-sum(sig(200:500)));  % Energy in frq. 200-500 div. by rest

RB_2 = sum(sig(1:800))/...
    (sum(sig)-sum(sig(1:800)));  % Energy in frq. 1-800 div. by rest

%%% Skewness and Kurtosis
skew = skewness(sig);            % Caluclate the skewness of the signal
kurt = kurtosis(sig);            % Calculate the kurtosis of the signal

end

