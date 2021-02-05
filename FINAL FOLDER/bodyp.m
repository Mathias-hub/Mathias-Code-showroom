function [posVec] = bodyp(X,Y,Z)
% "bodyp_func" is a function that calculates the
% current body posture using accelerometer data.
%
% INPUT: X,Y,Z         = Vectors with accelerations
%                        in X, Y and Z axis.
%
% OUTPUT: postVec       = Vector with positions for
%                         each timestep.
% Original code in JAVA: Casper B. Jespersen, Mads 
%                        Olsen & Steen Lillelund
%
%   MATLAB: Mathias P. Bonnesen, s113918, Technical University of Denmark. 
%   
%   Date: 12/23 - 2016
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%% INFO Regarding positions
% Position:  1: Surpine
%            2: Prone
%            3: Left lateral
%            4: Right lateral
%            5: Nothing to declare
%           -2: upright
%%%
%%% INITIATION ###
init = 0;          % 0 = false, 1 = true
w = 5;             % Length of a window
posVec = [];       % Vector with positions
c = 1;              % Counts

%%% GENERATE DATA STRUCTURE %%%
C_acc_data = zeros(3,length(X));
C_acc_data(1,:) = X;
C_acc_data(2,:) = Y;
C_acc_data(3,:) = Z;

for k = 1:length(X)-w
    acc_data = C_acc_data(:,k:k+w);
    
% Moving average window
    if init == 0
        x_sum = 0;
        y_sum = 0;
        z_sum = 0;
        init = 1;

        % Sum of values inside the window
        for ii = 1:w
            x_sum = x_sum + acc_data(1,ii);
            y_sum = y_sum + acc_data(2,ii);
            z_sum = z_sum + acc_data(3,ii);
        end

        % Window Average
        x_sum = x_sum/w;
        y_sum = y_sum/w;
        z_sum = z_sum/w;

        if abs(x_sum) > abs(y_sum) && abs(x_sum) > ...
                abs(z_sum)
            if x_sum < 0
                statusposition = 3; %% left lateral
            else
                statusposition = 4; %% right lateral
            end
        elseif abs(y_sum) > abs(x_sum) && abs(y_sum) > ...
                abs(z_sum)
            statusposition = -2; % upright
        elseif abs(z_sum) > abs(x_sum) && abs(z_sum) > ...
                abs(y_sum)
            if z_sum > 0
                statusposition = 1; % Surpine
            else
                statusposition = 2; % prone
            end
        else
            statusposition = 5; % Nothing to declare
        end
    end
    posVec(c) = statusposition;
    c = c + 1;
    init = 0;
end    

end