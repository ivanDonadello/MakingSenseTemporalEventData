# Supplementary material for "Making Sense of Temporal Event Data: a Framework for Comparing Techniques for the Discovery of Discriminative Process Patterns"

Submitted at CAiSE 2024.

This supplementary material contains the DECLARE models obtained from the methods described in the paper. In addition, a Java repository
is provided for the computation of the models accuracies for each event log. The event logs are publicly available for download and they are referenced in the paper.

## Requirements
The Java 17 code uses the Ivy package manager to download the required libraries.

## Description of the supplementary material
The supplementary material contains the following folders:
- `input` contains the computed DECLARE models for each event log and for each method.
- `src` contains the Java code for the metrics computations. The main file to launch is `src/makingSenseTemporalData.main/RunAccuracyExperiments.java`
- `results` contains the data reported in Table 1, 2, 3 in the paper.
