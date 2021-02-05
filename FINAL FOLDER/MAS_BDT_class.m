%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%   This function classifies an input matrix containing feature and labels,
%   and makes a Bagged Decision Tree supervized learning classification on the
%   data.  The MATLAB app "Classification Learner" was used to create the
%   frame of the code.
%
%   Input:
%       trainingData:  MxN feature matrix, where M is sample size, and N is
%                      features. Last row of features must be labels.
%       learners:      Number of learners used in the ensamble method (Int)
%       Classes:       Vector with classes, Ex. [0; 1; 2; 3]
%       K:             K-fold cross-validation integer
%
%   Output:
%       Classifier:    Object containing the trained classifier. This can
%                      be used to test on new data. The sub-structure
%                      Classifier.predictFcn is used for this.
%
%       Accuracy:      The accuracy of the trained classifier.
%       Predictions:   The predictions of the data, made by the classifier
%       Scores:        In case of RF or multi-class SVM, this illustrates
%                      the scoring made be the classifiers used to make the
%                      prediction.
%
%   Author: Mathias P. Bonnesen, s113918, Technical University of Denmark. 
%   
%   Date: 12/23 - 2016
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
function [Classifier, Accuracy,Predictions, Scores]...
    = MAS_BDT_class(trainingData,learners,classes,K)

% Extract features into a predictor vector 
predictors = trainingData(:,1:end-1);

% Extract labels into a response vector
response = trainingData(:,end);


% Create Cost matrix and assign 2x to increase cost of True Negative
vec1 = ones(1,length(classes));
cost = ones(length(classes),length(classes)) - diag(vec1);
cost(2:length(classes),1) = 2;

% Train a classifier
% This code specifies all the classifier options and trains the classifier.
classificationEnsemble = fitensemble(...
    predictors, ...
    response, ...
    'Bag', ...
    learners, ...
    'Tree', ...
    'Type', 'Classification', ...
    'ClassNames', classes,'Cost',cost);

% Create the result struct with predict function
predictorExtractionFcn = @(x) array2table(x, 'VariableNames', predictorNames);
ensemblePredictFcn = @(x) predict(classificationEnsemble, x);
Classifier.predictFcn = @(x) ensemblePredictFcn(predictorExtractionFcn(x));

% Add additional fields to the result struct
Classifier.ClassificationEnsemble = classificationEnsemble;

% Perform cross-validation
partitionedModel = crossval(Classifier.ClassificationEnsemble, 'KFold', K);

% Compute validation accuracy
Accuracy = 1 - kfoldLoss(partitionedModel, 'LossFun', 'ClassifError');

% Compute validation predictions and scores
[Predictions, Scores] = kfoldPredict(partitionedModel);
