%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Script which performs CLASSIFICATION and VALIDATION to a dataset.
%
% This script starts by loading the feature vectors and then performs a
% normalization. Afterwards the AHI-indexes for each subject is labelled
% using binary or multi-class labels, and merged with the feature vector
% into one "training_data.mat". This is used for classifying the data using
% a SVM model and Random Forest.
%
% The function (non-MATLAB) created for use in the script are:
% RandomnF_class:       Performs Random Forest classification on a dataset,
%                        and outputs classification results.
% MAS_SVM_class:        Performs SVM classification on a dataset, and
%                       outputs classification results.
%
% Author: Mathias P. Bonnesen, s113918, Technical University of Denmark. 
%   
% Date: 12/23 - 2016
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%% CREATE TRAINING MATRIX %%%
% Load feature structure
disp('Initializing Classification...')
load('FEATURES_sctruct.mat')
% Struct with ID's of all Patients included in the classification
Patvec = {'Pat_1';'Pat_2';'Pat_3';'Pat_4';'Pat_5';'Pat_6';...
    'Pat_10';'Pat_12';'Pat_13';'Pat_14';'Pat_15';...
    'Pat_18';'Pat_19';'Pat_22';'Pat_23';'Pat_24';'Pat_25';'Pat_26'...
    ;'Pat_29';'Pat_30';'Pat_31';'Pat_32';'Pat_33'};

% Assign AHI values to vector
AHI_val = [7.2;46.1;36.1;23.2;14.6;51.2;6.5;82.6;...
    21;4;13.1;5;3.1;37.1;32;25.8;4;40;59.8;2.6;42.4;16.3;5.3]';



AHIC_2 = repmat(AHI_val,3,1);

AHIC_4 = AHI_val;

% Convert values to 2-class labels
thr = [5,10,15];
for i = 1:3
    AHIC_2(i,AHIC_2(i,:)<=thr(i)) = 0;
    AHIC_2(i,AHIC_2(i,:)>thr(i)) = 1;
end

% Convert Values to 4-class labels
AHIC_4(AHIC_4<=5) = 0;
AHIC_4(AHIC_4>30) = 3;
AHIC_4(AHIC_4>15) = 2;
AHIC_4(AHIC_4>5) = 1;

resMat = zeros(4,4);

for it = 1:4
    it
    
    % Merge the features with the labels
    featmat = zeros(length(Patvec),length(feat_struc.Pat_1)+1);
    %featmat(:,5:end)
    
    nr_class = 1;

    if it < 4
        AHI_labels = AHIC_2(it,:);
    else
        AHI_labels = AHIC_4;
    end

    for s = 1:length(Patvec)
        pt = Patvec{s};
        featmat(s,:) = [feat_struc.(pt) AHI_labels(s)];    
    end
    
    % Control which features are used for classification
    %featmat=featmat(:,11:end);
    
    % General parameters
    classes = [0:max(featmat(:,end))]';
    K = 20;

    %featmat = featmat(:,6:end)

    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    %%% Bagged Decision Tree %%%

    % RF specific parameters
    if nr_class == 1
        nr_trees = 50; % Number of decision trees in the ensamble classifier
    else
       nr_trees = 50;
    end

    % Classification
    [ClassifierRF,AccRF,PredictRF, ScoresRF] ...
        = MAS_BDT_class(featmat,nr_trees,classes,K);

    CMAT_RF = confusionmat (featmat(:,end),PredictRF);
    
    Acc_RF = trace(CMAT_RF)/sum(sum(CMAT_RF))*100;
    Sens_RF = sum(CMAT_RF(1,1))/sum(CMAT_RF(:,1))*100;

    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    %%% SUPPORT VECTOR MACBHINE %%%

    % SVM specific parameters
    [ClassifierSVM, AccSVM, PredictionsSVM, ScoresSVM] ...
        = MAS_SVM_class(featmat,classes,K);

    %[Scores,featmat(:,end)]

    CMAT_SVM = confusionmat (featmat(:,end),PredictionsSVM)

    Acc_svm = trace(CMAT_SVM)/sum(sum(CMAT_SVM))*100;
    Sens_svm = sum(CMAT_SVM(1,1))/sum(CMAT_SVM(:,1))*100;
    
    resMat(it,:) = [Acc_RF,Sens_RF,Acc_svm,Sens_svm];
    
     load('Validation_Matrix','validation')
     switch it
         case it == 1
             validation.SC1 = {CMAT_RF,PredictRF;CMAT_SVM,PredictionsSVM}
         case it == 2 
             validation.SC2 = {CMAT_RF,PredictRF;CMAT_SVM,PredictionsSVM}
         case it == 3
             validation.SC3 = {CMAT_RF,PredictRF;CMAT_SVM,PredictionsSVM}
         case it == 4
             validation.MC = {CMAT_RF,PredictRF;CMAT_SVM,PredictionsSVM}
         end
     save('Validation_Matrix','validation')
end
