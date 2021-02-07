%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% This script compares the events found by the medical staff and the events
% located by MAS algorithm
%
% Author: Mathias P. Bonnesen, s113918, Technical University of Denmark. 
%   
% Date: 12/23 - 2016
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% LOAD and store events

current_folder = '\\10.230.149.201\MobilApnea\MATLAB'
filepath = ['\\10.230.149.201\MobilApnea\DATA\', Pat, '\crm'];

filename = [filepath,'\event_matrix.txt']

fileID = fopen(filename,'r');        % read whole file
textfile = textscan(fileID,'%s','Delimiter','\n');
textfile = textfile{:};
event_matrix = cell(1,3);     % Create event matrix cell structure
count = 1;

for ii = 1:length(textfile)
   cellf = strsplit(textfile{ii},';');
   cellf = cellf{:}; 
   if ii == 1
    starth_rec = str2num(cellf(24:25));        % event start, H,m,s
    startm_rec = str2num(cellf(27:28));            
    starts_rec = str2num(cellf(30:31));  
    
    
    add_sec = abs(starth_rec*3600+startm_rec*60+starts_rec - 24*3600);
    
   elseif ii > 3
       cellf = strsplit(textfile{ii},' ');
       time = cellf{end - 3};
       start_h = str2num(time(1:2));        % event start, H,m,s
       start_m = str2num(time(4:5));            
       start_s = str2num(time(7:8));        
       
           % See if the event is before midnight
        if start_h > 10;
            event_sample = add_sec + ((start_h+start_m/60+start_s/3600)-24)*3600;
        else
            event_sample = start_h*3600+start_m*60+start_s + add_sec;
        end     
       
        cellf2 = strsplit(cellf{end-1},',');
       duration_s = [str2num(cellf2{1});str2num(cellf2{end})];
       duration = duration_s(1) + ceil(duration_s(end)/10); % event duration
       
       event_matrix{count,1} = int64(event_sample);              % start of event
       event_matrix{count,2} = int64(event_sample) + duration;   % end of event
       event_matrix{count,3} = cellf2{1};
       event_matrix{count,4} = cellf{1};
       count = count + 1;
       
   end
   
end
cd(current_folder)

%% PLOT Aud, Acc, Label and CRM results
window = 50;
crm_lab = zeros(1,floor(length(X_f)/fs_acc));
c = 0;

for i =300:size(event_matrix,1)
%for i = 702:size(event_matrix,1)    
    if event_matrix{i,1} > -LAG ...
            && event_matrix{i,1}+LAG < length(X_f)/fs_acc
        
        start = event_matrix{i,1} + LAG;
        stop = event_matrix{i,2} + LAG;
        
        Inta = start*fs_aud-fs_aud*window:...
            stop*fs_aud + fs_aud*window;  % Look at events

        Intb = start*fs_acc-fs_acc*window:...
            stop*fs_acc + fs_acc*window;  % Look at events
%        m = max(audio_p(Int));
       [start stop];
        event_type = event_matrix{i,4}
         if strcmp(event_type,'Apnea') == 1;
             crm_lab(start:stop) = ones(length(start:stop),1)*(-1.5);
%              disp('bing')
                c = c + 1
%              pause
         elseif strcmp(event_type,'Hypopnea')
             %c = c + 1
             %crm_lab(start:stop) = ones(length(start:stop),1)*(-2);
         end
        
        aud_env = envelope(audio_p(Inta),fs_aud/2,'rms');
        figure(1)
        subplot(4,1,1)
        title(event_type)
        hold off
        plot(0:1/fs_aud:length(audio_p(Inta))/fs_aud-1/fs_aud,audio_p(Inta))
        title('Audio')
        hold on
        plot(0:1/fs_aud:length(audio_p(Inta))/fs_aud-1/fs_aud,lab_vec(Inta)*400);
        axis tight
        subplot(4,1,2)
        hold off
        plot(0:1/fs_acc:length(c_acc(Intb))/fs_acc-1/fs_acc,X_f(Intb));
        title('X Acc')
        axis tight
        subplot(4,1,3)
        hold off
        plot(0:1/fs_acc:length(c_acc(Intb))/fs_acc-1/fs_acc,Y_f(Intb));
        title('Y Acc')
        axis tight
        subplot(4,1,4)
        hold off
        plot(0:1/fs_acc:length(c_acc(Intb))/fs_acc-1/fs_acc,Z_f(Intb));
        title('X Acc')
        xlabel('Time (seconds)')
        axis tight
        figure(2)
        plot(rest_vec(1:end-3,start-window:stop+window)')
        %legend('RW_25','RW_5e','RW_18','RB_1','RB_2')
        axis tight
        legend('RW_25','RW_5e','RW_18')
        figure(3)
        hold off
        plot(pAvg_vec(start-window:stop+window))
        hold on
        plot(se_vec(start-window:stop+window))
        axis tight
        set(gca,'yscale','log','fontsize',18);
        legend('pAvg','Entropy')
        pause
    end
end







