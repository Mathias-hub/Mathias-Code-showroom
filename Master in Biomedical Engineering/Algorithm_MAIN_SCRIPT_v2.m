%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Main script that loads the data, performs preprocessing and stores the
% data ready for feature extraction. Then it extracts features and saves
% them into a feature matrices. Also loads data from CRM analysis
%
%   Loads: audio, X, Y, Z, fs_acc and fs_aud
%
%   Outputs:   feat_struc:          Struct with features for each patient
%              all_feat:            Vectors used to extract features
%
%   Author: Mathias P. Bonnesen, s113918, Technical University of Denmark. 
%   
%   Date: 01/05 - 2016
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% SELECT FOLDER AND LOAD DATA %%%
Patvec = {'Pat_1';'Pat_2';'Pat_3';'Pat_4';'Pat_5';'Pat_6';'Pat_8';...
    'Pat_9';'Pat_10';'Pat_12';'Pat_13';'Pat_14';'Pat_15';'Pat_16';...
    'Pat_18';'Pat_19';'Pat_22';'Pat_23';'Pat_24';'Pat_25';'Pat_26';...
    'Pat_29';'Pat_30';'Pat_31';'Pat_32';'Pat_33'}

Patvec = {'Pat_32'}

MODE = 'no'

% Loop through each patient form Patvec
for k = 1:size(Patvec,1)
    Pat = Patvec{k,:};
    main_folder = '\\10.230.149.201\MobilApnea\MATLAB';  % MAIN FOLDER
    cd(main_folder)

    folder = ['\\10.230.149.201\MobilApnea\DATA\'  Pat]; % Files location
    
    % Look if patient have been processde previously
    if isempty(dir([folder '\matlab_data' '\*mat'])) == 1  
        disp([Pat ': No .mat files. Creating .mat files...'])

        % Load data
        tic
        [X,Y,Z,fs_acc,audio,fs_aud] = load_data(folder);
        toc
        
        % Load CRM data to find start and stop
        crm_fold = ['\\10.230.149.201\MobilApnea\DATA\' Pat '\crm'];
        [LAG] = CRM_data_extract(crm_fold); % Find lag between MAS/CRM and
                                            % create event_matrix
        
        cd([folder '\crm'])
        
        % Segment to extract Time window segment
        fileID = fopen('event_matrix.txt','r');        % read whole file
        textfile = textscan(fileID,'%s','Delimiter','\n');
        textfile = textfile{:};
        cellf_start = strsplit(textfile{2},';');
        cellf_stop = strsplit(textfile{1},';');
        time_start = cellf_stop{1};
        time_stop = cellf_start{1};
        crm_start = str2num(time_start(24:25))*3600 + str2num(time_start(27:28))*60 ...
            + str2num(time_start(30:31)) - 24*3600;
        rec_length = str2num(time_stop(11:12))*3600+str2num(time_stop(14:15))*60 ...
            + str2num(time_stop(17:18)) - crm_start;

        % This ensures that the MAS analysis stops when "lights is set ON"
        rec_interval = length(-LAG:rec_length);  % Length in seconds
        
        % Address inconsistencies in data of Pat 1 and Pat 24
        if strcmp(Pat,'Pat_1') == 1
            rec_interval = length(600:25890); % Adjust clock-miss-set on Pat 1
            LAG = 0;
        elseif strcmp(Pat,'Pat_24') == 1
            rec_interval = length(600:length(audio)/fs_aud); % Take whole signal post 10min
            LAG = 0;
        end
        
        start = 0;  % Time of analysis start.

        if length(-LAG:rec_length) > length(X)/fs_acc        
            X = X(fs_acc*start+1:end); Y = Y(fs_acc*start+1:end); ...
            Z = Z(fs_acc*start+1:end); audio = audio(fs_aud*start+1:end);
        else
            X = X(fs_acc*start+1:rec_interval*fs_acc); Y = Y(fs_acc*start+1:rec_interval*fs_acc); ...
            Z = Z(fs_acc*start+1:rec_interval*fs_acc); audio = audio(fs_aud*start+1:rec_interval*fs_aud);
        end

        cd(main_folder)
        
        %%%%%%%%%%%%%%%%%%%%%%%%
        % PREPROCESSING
        %%%%%%%%%%%%%%%%%%%%%%%
        disp('Stating preprocessing of signals...')
        % Audio preprocessing
        tic
       
        % AUDIO
        Fpass1 = 200;        % First Passband Frequency
        Fpass2 = 1200;        % Second Passband Frequency
        order = 40;

        d = designfilt('bandpassiir','FilterOrder',order, ...
             'HalfPowerFrequency1',Fpass1,'HalfPowerFrequency2',Fpass2, ...
             'SampleRate',fs_aud);

        audio_p=filtfilt(d,audio);
       
        % ACCELERATIONS
        Fpass1 = 0.10;  Fpass2 = 0.80;        % Passband Frequencies, - RESP
        Fpass1h = 0.8;  Fpass2h = 2.0;        % Passband Frequencies, - HEART
        order = 40;

        d2 = designfilt('bandpassiir','FilterOrder',order, ...
            'HalfPowerFrequency1',Fpass1,'HalfPowerFrequency2',Fpass2, ...
            'SampleRate',fs_acc);

        X_f=filtfilt(d2,X);
        Y_f=filtfilt(d2,Y);
        Z_f=filtfilt(d2,Z);

        % Filter for HEART frequency
        d3 = designfilt('bandpassiir','FilterOrder',order, ...
            'HalfPowerFrequency1',Fpass1h,'HalfPowerFrequency2',Fpass2h, ...
            'SampleRate',fs_acc);

        X_fh=filtfilt(d3,X);
        Y_fh=filtfilt(d3,Y);
        Z_fh=filtfilt(d3,Z);
        
        info = [fs_acc;fs_aud;LAG+start];
        
        posvec = bodyp(X,Y,Z);  % Position is calculated here, since it
                                % the raw accelerometer data.

        % Save procesed signals
        cd([folder '\matlab_data'])
        save('X_f','X_f'); save('Y_f','Y_f'); save('Z_f','Z_f');
        save('X_fh','X_fh'); save('Y_fh','Y_fh'); save('Z_fh','Z_fh')
        save('posvec','posvec'); save('audio_p','audio_p')
        save('info','info')
        
        cd(main_folder)
    else
        % If Pat have previosly been precossed, just load files.
        disp([Pat ': .mat files located. Loading...'])
        cd([folder '\matlab_data'])
        % Load the cut_signal matfile
        load('X_f','X_f'); load('Y_f','Y_f'); load('Z_f','Z_f');
        load('X_fh','X_fh'); load('Y_fh','Y_fh'); load('Z_fh','Z_fh')
        load('posvec','posvec'); load('audio_p','audio_p')
        load('info','info')
        fs_acc = info(1); fs_aud = info(2); LAG = info(3);
        cd(main_folder)
    end

    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    % FEATURE EXTRACTION
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    disp('Feature extraction beginning...')
    
    if strcmp(MODE,'surpine') == 1
        posvec_a = ones(1,floor(length(audio_p)/fs_aud));
        for tv = 2:length(X_f)/fs_acc-1
           posvec_a(tv*fs_aud:(tv+1)*fs_aud-1) = ...
               ones(1,fs_aud)*posvec((tv-1)*25);
        end
        
        disp('Surpine calculations')
        X_f = X_f(posvec==1); Y_f = Y_f(posvec==1); Z_f = Z_f(posvec==1); 
        audio_p = audio_p(posvec_a==1); 
    end
    
    %%%%% Computing the Signal Vector Magnitude %%%%%
     c_acc = sqrt(X_f.^2+Y_f.^2+Z_f.^2);  % Composite accelerations for Actiography
     
    % Label windows
    w = 1;                                            % Window size.
    ov = 1;                                         % Overlap [0:1]
    a_window = 0;                                   % Was 1800 in prior calc
    
    
    %%% Feature vectors %%%
    pAvg_vec = zeros(1,length(a_window:1/w:length(audio_p)/fs_aud));
    se_vec = zeros(1,length(a_window:1/w:length(audio_p)/fs_aud));
    ZCR_vec = zeros(1,length(a_window:1/w:length(audio_p)/fs_aud));
    mov_vec = zeros(1,length(a_window:1/w:length(audio_p)/fs_aud));
    rest_vec = zeros(8,length(a_window:1/w:length(audio_p)/fs_aud));
    posvec_sec = zeros(1,length(a_window:1/w:length(audio_p)/fs_aud));
    
    %%% Label vectors
    lab_vec2 = zeros(1,length(a_window:1/w:length(audio_p)/fs_aud));
    
    % Calculate spectral features from window.
    for i = 1:ov:length(audio_p)/fs_aud - 5
        au_seg = audio_p(i*fs_aud:i*fs_aud+fs_aud*w);
        acc_seg = i*fs_acc:i*fs_acc+fs_acc*w;

        [pAvg,fp,se,RW_25,RW_5e,RW_18,RB_1,RB_2,skew,kurt]...
            = spec_feat(pwelch(au_seg*w,fs_aud*w));

        % mov_T
         mov = quantile(c_acc(acc_seg),0.50);            % Quantile
         
         % Zero-croos-rate
        ZCR=mean(abs(diff(sign(au_seg))));

        pAvg_vec(i) = pAvg;
        se_vec(i) = se;
        ZCR_vec(i) = ZCR;
        mov_vec(i) = mov;
        rest_vec(:,i) = [fp,RW_25,RW_5e,RW_18,RB_1,RB_2,skew,kurt];
    end

 %%%%% Calculate Features %%%%%
    %%% EVENT METHOD 1 %%%
    MAS_AHI = zeros(1,4);
    cs = 1;
    w = 1; ov = 1;
    disp('Labelling windows...')

    for ahi_t = [5*10^3,quantile(pAvg_vec,0.60),quantile(pAvg_vec,0.50),...
            quantile(pAvg_vec,0.40)]
        lab_vec = ones(length(audio_p),1);   % Label vector
        event_vec = zeros(length(audio_p),1);

        c = 0;
        c_val2 = 0;
        c_val = 0;
        
        for i = 1:length(pAvg_vec)
            pAvg = pAvg_vec(i);

             if (pAvg)*w < (ahi_t)*w %|| (pAvg)*w < se %RW_25 < 0.5%&& se < seT/10
               lab_vec(i*fs_aud:i*fs_aud+fs_aud) = zeros(length(fs_aud),1); 
               c_val = c_val + 1;
               if c_val > 9*(1/(w*ov))
                   event_vec(i*fs_aud-9*fs_aud:i*fs_aud+fs_aud)=ones(length(fs_aud),1)*1;
                   lab_vec(i*fs_aud-9*fs_aud:i*fs_aud+fs_aud) = ones(length(fs_aud),1)*(-1); 
                   c = c + 1;
               end
            elseif pAvg > 10^8 % && se > seT*100
               lab_vec(i*fs_aud:i*fs_aud+fs_aud) = ones(length(fs_aud),1)*2;
               if c_val > 9; c_val2 = c_val2 + 1; end
               c_val = 0;

            else
                lab_vec(i*fs_aud:i*fs_aud+fs_aud) = ones(length(fs_aud),1);
               if c_val > 9; c_val2 = c_val2 + 1; end
                c_val = 0;
            end

           lab_vec2(i) = unique(lab_vec(i*fs_aud:i*fs_aud));
        end
        
        %%% Event Method 1 - finds apneic moments
        MAS_AHI(cs) = c_val2/(length(X_f)/(fs_acc*3600));
        cs = cs+1;
    end
    
    
    %%%%% Event Method 2 %%%%
        sam_vec_pAvg = zeros(length(pAvg_vec),1);
        event_vec_pAvg = zeros(length(pAvg_vec),1);

        for sample = 20:length(sam_vec_pAvg)-11
            sumAf = sum(log(pAvg_vec(sample:sample+9)))/10;
            sumBe = sum(log(pAvg_vec(sample - 10:sample-1)))/10;
            sam_vec_pAvg(sample) = sumAf/sumBe;
        end
        
        disp('Calculating MAS_AHI type 2...')
        MAS_AHI2 = zeros(1,4);
        mc = 1;
        for Thr = [quantile(sam_vec_pAvg,0.99),1.8,2,2.5]
            loopvec = event_vec_pAvg;
            [pks,locs] = findpeaks(sam_vec_pAvg); % Find peaks aboive 1.8 Threshold
            loopvec(locs(pks>Thr)) = 0.5;
            
            % Implement min peaks distance on the threshold-peaks
            [pks,locs] = findpeaks(loopvec,'MinPeakDistance',9);
            loopvec(locs(diff(locs)>9)) = 1;
            loopvec(loopvec==0.5) = 0;
            
            % Calculate MAS_AHI
            MAS_AHI2(mc) = sum(abs(loopvec))/(length(pAvg_vec)/3600);
            mc = mc + 1;
        end
        
    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    
    %%% QUESTIONAIRE features %%%
    filename = [folder,'\iFile.txt'];
    ifile_ID = fopen(filename,'r');        % read whole file
    textfile = textscan(ifile_ID,'%s','Delimiter','\n');
    textfile = textfile{:};

    % BMI
    bmi_answ = strsplit(textfile{1},';');
    bmi_answ = bmi_answ{:}; 
    bmi = (str2num(bmi_answ(20:end-1)))/(str2num(bmi_answ(8:10))/100)^2;
    
    % Rest of answers
    answ = strsplit(textfile{5},';');
    answ = answ{1};
    Q_answ = [];
    for s = [20,27,30,33,36,39,42]
            val = str2num(answ(s))-1;
            Q_answ = [Q_answ; val];
    end
    % Save age into variable
    Age = str2num(answ(23:24));
    % Calculate OSA severity based on Q's
    Q_answ(2) = fix(str2num(answ(23:24))/50); % 'fix' ignores decimal val
    Q_sev = sum(Q_answ) + fix(bmi/35);

    
    %%% Apnea Movement minutes %%
    c_acc_s = zeros(1,ceil(length(c_acc)/fs_acc));
    cs = 1;
    
    for ii = 1+fs_acc:fs_acc:length(c_acc)
        c_acc_s(cs) = rms(c_acc(ii-fs_acc:ii));
        cs = cs+1;
    end
    
    mov_T = quantile(c_acc_s,0.95);            % Accelerometer Thresholds
    apn_mov = length(c_acc_s(c_acc_s>mov_T));
    
    %%% Percentage in surpine position
    surpine = sum(posvec==1)/length(posvec);
    
    %%% Position in seconds
    posvec_sec = posvec(1:25:end);
    
    % Respiration rate analysis
    w = 30; % Window size for analysis
    [rr_vecX,rr_vecY,rr_vecZ] = rr_analysis(X_f,Y_f,Z_f,fs_acc,w);
    rrQy = quantile(diff(rr_vecY),[0.10 0.90]);  % Thresholds
    rrQz = quantile(diff(rr_vecY),[0.10 0.90]);  % Thresholds
    RR_peaks = [sum(diff(rr_vecY)<rrQy(1))+ sum(diff(rr_vecY)>rrQy(2)) ...
        sum(diff(rr_vecZ)<rrQz(1))+ sum(diff(rr_vecZ)>rrQz(2))];
    
    % Snore variability analyzes snoring
    snores_pAvg = log(pAvg_vec(lab_vec2==2));   % LOG of snore power
    
    % Apnea snoring minutes
     sn_thresh = 900;
     sn_min = sum(rest_vec(1,:)>900);

    % remove a field: 
    %feat_struc = rmfield(feat_struc,'Pat_25')

    %%%%%%%%%%%%%%%%%%%%%%%%%%%
    % SAVE INTO Feature Vectors
    %%%%%%%%%%%%%%%%%%%%%%%%%%%
    
    if strcmp(MODE,'surpine') == 0
        load('FEATURES_sctruct.mat')       % Load feature structure
        load('ALL_FEATURES','all_feat')

        feat_struc.(Pat) = [MAS_AHI MAS_AHI2 apn_mov sn_min surpine bmi Age Q_sev]
        all_feat.(Pat) = [pAvg_vec;se_vec;rest_vec;event_vec_pAvg'];

        save('ALL_FEATURES','all_feat')
        save('FEATURES_sctruct','feat_struc')
    else
        load('FEATURES_sctruct_surp.mat')       % Load feature structure
        load('ALL_FEATURES_surp','all_feat')

        feat_struc.(Pat) = [MAS_AHI MAS_AHI2 apn_mov sn_min surpine bmi Age Q_sev]
        all_feat.(Pat) = [pAvg_vec;se_vec;rest_vec;lab_vec2;event_vec_pAvg'];

        save('ALL_FEATURES_surp','all_feat')
        save('FEATURES_sctruct_surp','feat_struc')
    end
    
    % posvec_sec; has been removed
    disp([Pat '... Done'])

end
