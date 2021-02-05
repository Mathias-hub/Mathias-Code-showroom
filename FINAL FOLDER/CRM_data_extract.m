%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Function that extract information from the CRM analysis .txt files. The
% "Markers.txt" is used to calculate the LAG between MAS and CRM recording
% start times. Furthermore, the function creates a matrix
% "event_matrix.txt" containing information of each annotated event.
%
% INPUT
%   filepath:           - Folder where crm-data is located. The .txt files
%                       must must only contain time, not date.
%   OUTPUT
%   LAG                 - The difference (in samples) between MAS and CRM
%                       start
%
%   Author: Mathias P. Bonnesen, s113918, Technical University of Denmark. 
%   
%   Date: 11/21 - 2016
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


function [LAG] = CRM_data_extract(filepath)
current_folder = cd;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% Find lag between CRM and MAS %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%%%%%%% Calculate LAG %%%%%%%%%
cd(filepath)

fileID = fopen('Markers.txt','r');        % read whole file
textfile = textscan(fileID,'%s','Delimiter','\n');
textfile = textfile{1};
time = textfile{2};

time_mo = str2num(time(16:17));
time_d = str2num(time(13:14));
time_h = str2num(time(24:25));
time_m = str2num(time(27:28));
time_s = str2num(time(30:31));

MAS_data = readtable([filepath(1:end-3) 'iFile.txt']);  % READ MAS time

time_moM = str2num(MAS_data{8,4}{1}(4:5));        % MAS start time (Month)
time_dM = str2num(MAS_data{8,4}{1}(1:2));        % MAS start time (Day)
time_hM = MAS_data{8,5};                         % MAS start time (Hour)
time_mM = str2num(MAS_data{9,1}{1}(2:3));        % MAS start time (Minute)
time_sM = str2num(MAS_data{9,1}{1}(5:6));        % MAS start time (Second)


date_check = (str2num(time(13:14))+str2num(time(16:17))*100) - ...
    (str2num(MAS_data{8,4}{1}(1:2)) + str2num(MAS_data{8,4}{1}(4:5))*100);

mo_diff = time_mo - time_mM;                          % Time difference
d_diff = time_d - time_dM;                          % Time difference
h_diff = time_h - time_hM;                          % Time difference
m_diff = time_m - time_mM;
s_diff = time_s - time_sM;

% Find TOTAL LAG in seconds
LAG = h_diff * 3600 + m_diff * 60 - s_diff...
    - 24*3600*(sign(m_diff)*sign(d_diff));   % Find MAS/CRM LAG (seconds)
Duration = 1;

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%% Find and store events %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%%%%%%%%%

event_matrix = cell(1,4);     % Create event matrix cell structure
count = 4;                    % Variable to keep track of event number
txtfiles = dir('*.txt');

    for i = 1:length(txtfiles)               % Loop through all .txt files
        filename = txtfiles(i).name;
        
        if isempty(strfind(filename,'Event')) == 0  % Neglect blank lines            
            fileID = fopen(filename,'r');        % read whole file
            textfile = textscan(fileID,'%s','Delimiter','\n');
            textfile = textfile{:};            
            for ii = 1:length(textfile)
               cellf = strsplit(textfile{ii},';');               
               if length(cellf) > 1          
                   time = cellf{1};         % Extract time
                   
                   start = time(1:12);               % event start
                   stop = time(14:end);              % event stop
                   duration = cellf{2};              % duration
                   label = cellf{3};                 % event label
                   
                   % Save them into a structure
                   event_matrix{count,1} = label;
                   event_matrix{count,2} = start;
                   event_matrix{count,3} = stop;
                   event_matrix{count,4} = duration;
                   count = count + 1;
               end
            end
            
            % When event .txt file is encountered, extract analysis start
            % time and lights ON
        elseif isempty(strfind(filename,'Markers')) == 0
            
            % START TIME
            fileID = fopen(filename,'r');        % read whole file
            textfile = textscan(fileID,'%s','Delimiter','\n');
            textfile = textfile{:};
            cellf = strsplit(textfile{2},';');
            timec = cellf{1};
            event_matrix{1,1} = timec(1:11);
            event_matrix{1,2} = timec(13:22);
            event_matrix{1,3} = timec(24:end);
            
            % END TIME
            textfile_stop = textfile{end};
            cellf = strsplit(textfile_stop,';');
            event_matrix{2,1} = textfile_stop(15:end);
            event_matrix{2,2} = textfile_stop(1:12);
        end
    end
        
    %%%% Write data into .txt file
        fileID2 = fopen('event_matrix.txt','w');
        formatSpec = '%s %s %s %s \n';
        [nrows,ncols] = size(event_matrix);
    for row = 1:nrows
        fprintf(fileID2,formatSpec,event_matrix{row,:});
    end
    fclose(fileID);
    cd(current_folder)
end

