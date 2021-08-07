package com.example.potholedetectionapp;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.example.potholedetectionapp.libsvm.svm_model;
import com.example.potholedetectionapp.libsvm.svm;
import com.example.potholedetectionapp.libsvm.svm_node;
import com.example.potholedetectionapp.libsvm.svm_parameter;
import com.example.potholedetectionapp.libsvm.svm_problem;


public class SVM {
    static int record_count = 60;
    static int feature_count = 3;

    // Labels[num_records]
    static int[] labels = new int[record_count];

    // Feature[feature_index][feature_value]
    static double[][] features = new double[record_count][feature_count];

    //Create model that will train on datapoints
    static svm_model svm_model_data;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void main(String[]args) {

        readFile();

        svm_model_data = svmTrain(features, labels, 50);

        double[][] test_data = new double[10][3];

        for(int i = 0; i < 10; i++){
            test_data[i] = features[i+50];
        }

        double[] ypred = svmPredict(test_data, svm_model_data);

        for (int i = 0; i < test_data.length; i++){
            System.out.println("(Actual:" + labels[i+50] + " Prediction:" + ypred[i] + ")");
        }


    }



    public static svm_model svmTrain(double[][] xtrain, int[] ytrain, int train_size) {

        svm_problem prob = new svm_problem();
        prob.y = new double[train_size];
        prob.l = train_size;
        prob.x = new svm_node[train_size][feature_count];

        // For each record
        for (int instance = 0; instance < train_size; instance++) {
            double[] features = xtrain[instance];

            prob.x[instance] = new svm_node[feature_count];
            for (int feature = 0; feature < feature_count; feature++) {
                svm_node node = new svm_node();
                node.index = feature;
                node.value = features[feature];
                prob.x[instance][feature] = node;
            }

            // Set the label for this record.
            prob.y[instance] = ytrain[instance];
        }

        svm_parameter param = new svm_parameter();
        param.probability = 1;
        param.gamma = 0.5;
        param.nu = 0.5;
        param.C = 10;
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.cache_size = 20000;
        param.eps = 0.001;

        svm_model model = svm.svm_train(prob, param);

        return model;
    }


    public static double[] svmPredict(double[][] xtest, svm_model model) {

        double[] yPred = new double[xtest.length];

        for (int k = 0; k < xtest.length; k++) {
            double[] fVector = xtest[k];

            svm_node[] nodes = new svm_node[fVector.length];

            for (int i = 0; i < fVector.length; i++) {
                svm_node node = new svm_node();
                node.index = i;
                node.value = fVector[i];
                nodes[i] = node;
            }

            int totalClasses = 3;
            int[] labels = new int[totalClasses];
            svm.svm_get_labels(model, labels);

            double[] prob_estimates = new double[totalClasses];
            yPred[k] = svm.svm_predict_probability(model, nodes, prob_estimates);
        }

        return yPred;

    }

    /**
     *
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void readFile(){

        //String base = "/Users/schachte/Desktop/movement_output.txt";
        String base = "/Users/schachte/Desktop/movement_output.txt";

        //String currentPath = new java.io.File(".").getCanonicalPath();

        /*Paths may be used with the Files class to operate on files, directories, and other types of files.
        For example, suppose we want a BufferedReader to read text from a file "access.log".
        The file is located in a directory "logs" relative to the current working directory and is UTF-8 encoded. */
        //Path path = FileSystems.getDefault().getPath("logs", "access.log");
        Path path = FileSystems.getDefault().getPath("data", "data.txt");

        try {
            List<String> content;
            content = (Files.readAllLines(Paths.get(base)));

            String[] line_contents;
            String[] line_features;

            int current_record = 0;

            //For each line in file
            for(String line : content){
                line_contents = line.split(",");
                labels[current_record] = Integer.parseInt(line_contents[0]);

                line_features = line_contents[1].split("#");
                features[current_record][0] = Double.parseDouble(line_features[0]);
                features[current_record][1] = Double.parseDouble(line_features[1]);
                features[current_record][2] = Double.parseDouble(line_features[2]);

//                System.out.println("Label: " + labels[current_record]);
//                System.out.println("F1: " + features[current_record][0]);
//                System.out.println("F2: " + features[current_record][1]);
//                System.out.println("F3: " + features[current_record][2]);

                current_record++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        //Parse
        // For each feature in line

    }

}
