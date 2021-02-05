%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%   This function classifies an input matrix containing feature and labels,
%   and makes a Random Forest supervized learning classification on the
%   data. The MATLAB app "Classification Learner" was used to create the
%   frame of the code.
%
%   Input:
%       trainingData:  MxN feature matrix, where M is sample size, and N is
%                      features. Last row of features must be labels.
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
function [Classifier, Accuracy, Predictions, Scores] ...
    = MAS_SVM_class(trainingData,classes,K)

% Read features into matrix
predictors = trainingData(:,1:end-1);

% Assign class labels to response vector
response = trainingData(:,end);


%%% TRAINING THE SVM
% An if-loop determines if its a binary og multiclass classification
% problem
if length(classes) > 2
    % In case of a multi-class problem, multiple SVM's must be classified,
    % , and used for classification (one-vs-one or one-vs-all)    
    template = templateSVM(...
        'KernelFunction', 'linear', ...
        'PolynomialOrder', [], ...
        'KernelScale', 'auto', ...
        'BoxConstraint', 1, ...
        'Standardize', true);
    classificationSVM = fitcecoc(...
        predictors, ...
        response, ...
        'Learners', template, ...
        'Coding', 'onevsone', ...
        'ClassNames', classes);
else
    % In case of a binary classification, one SVM is created

    classificationSVM = fitcsvm(...
        predictors, ...
        response, ...
        'KernelFunction', 'linear', ...
        'PolynomialOrder', [], ...
        'KernelScale', 'auto', ...
        'BoxConstraint', 1, ...
        'Standardize', true, ...
        'ClassNames', classes);
end

% Create the result struct with predict function
predictorExtractionFcn = @(x) array2table(x, 'VariableNames', predictorNames);
svmPredictFcn = @(x) predict(classificationSVM, x);
Classifier.predictFcn = @(x) svmPredictFcn(predictorExtractionFcn(x));

% Add additional fields to the result struct
Classifier.ClassificationSVM = classificationSVM;

% Perform cross-validation
partitionedModel = crossval(Classifier.ClassificationSVM, 'KFold', K);

% Compute validation accuracy
Accuracy = 1 - kfoldLoss(partitionedModel, 'LossFun', 'ClassifError');

% Compute validation predictions and scores
[Predictions, Scores] = kfoldPredict(partitionedModel);
