package com.mycompany.mavenproject3;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.ArrayList;
import java.util.Comparator;

public class Main extends Application {

    public static class Process {
        int pid, arrival, burst, priority;
        Process(int pid, int arrival, int burst, int priority) {
            this.pid = pid; this.arrival = arrival; this.burst = burst; this.priority = priority;
        }
        public int getPid() { return pid; }
        public int getArrival() { return arrival; }
        public int getBurst() { return burst; }
        public int getPriority() { return priority; }
    }

    public static class ProcessResult {
        int pid, wt, tat, rt, firstStart;
        ProcessResult(int pid, int wt, int tat, int rt, int firstStart) {
            this.pid = pid; this.wt = wt; this.tat = tat; this.rt = rt; this.firstStart = firstStart;
        }
        public int getPid() { return pid; }
        public int getWt() { return wt; }
        public int getTat() { return tat; }
        public int getRt() { return rt; }
        public int getFirstStart() { return firstStart; }
    }

    public static class AvgRow {
        String algo; double wt, tat, rt;
        AvgRow(String algo, double wt, double tat, double rt) {
            this.algo = algo; this.wt = wt; this.tat = tat; this.rt = rt;
        }
        public String getAlgo() { return algo; }
        public double getWt() { return wt; }
        public double getTat() { return tat; }
        public double getRt() { return rt; }
    }

    class GanttEntry {
        int pid, start, end;
        GanttEntry(int pid, int start, int end) { this.pid = pid; this.start = start; this.end = end; }
    }

    ArrayList<Process> processes = new ArrayList<>();
    int[] finishTime, responseTime;
    ArrayList<GanttEntry> currentGantt = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        Label title = new Label("CPU Scheduling: SRTF vs Priority");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<Process> inputTable = new TableView<>();
        setupInputTable(inputTable);

        TextField pidF = new TextField(); pidF.setPromptText("PID");
        TextField arrF = new TextField(); arrF.setPromptText("Arrival");
        TextField burF = new TextField(); burF.setPromptText("Burst");
        TextField priF = new TextField(); priF.setPromptText("Priority");

        Button addBtn = new Button("Add Process");
        Button runBtn = new Button("Run & Compare");
        Button clearBtn = new Button("Clear All");
        runBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        clearBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

        
        Button btnA = new Button("Scenario A (Mixed)");
        Button btnB = new Button("Scenario B (Conflict)");
        Button btnC = new Button("Scenario C (Starvation)");
        Button btnD = new Button("Scenario D (Validation)");
        
        HBox scenarioBox = new HBox(10, new Label("Presets:"), btnA, btnB, btnC, btnD);
        scenarioBox.setPadding(new Insets(5));
       

        HBox inputBox = new HBox(10, pidF, arrF, burF, priF, addBtn, runBtn, clearBtn);
        inputBox.setPadding(new Insets(10));

        HBox ganttSRTF = new HBox(5);
        HBox ganttPriority = new HBox(5);
        TableView<ProcessResult> tableSRTF = new TableView<>();
        TableView<ProcessResult> tablePriority = new TableView<>();
        setupResultTable(tableSRTF);
        setupResultTable(tablePriority);

        TableView<AvgRow> avgTable = new TableView<>();
        setupAvgTable(avgTable);

        Label resLabel = new Label("Best Algorithm: -");
        resLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2980b9;");

        VBox left = new VBox(10, new Label("SRTF Results (Preemptive)"), ganttSRTF, tableSRTF);
        VBox center = new VBox(10, new Label("Priority Results (Preemptive)"), ganttPriority, tablePriority);
        VBox right = new VBox(10, new Label("Comparison Table"), avgTable, resLabel);

        HBox mainContent = new HBox(20, left, center, right);
        ScrollPane scrollPane = new ScrollPane(mainContent);

        VBox root = new VBox(10, title, scenarioBox, inputBox, inputTable, scrollPane);
        root.setPadding(new Insets(15));

       
        addBtn.setOnAction(e -> {
            try {
                validateAndAdd(pidF, arrF, burF, priF, inputTable);
            } catch (Exception ex) {
                showError("Validation Error", ex.getMessage());
            }
        });

        clearBtn.setOnAction(e -> {
            processes.clear();
            inputTable.getItems().clear();
            tableSRTF.getItems().clear();
            tablePriority.getItems().clear();
            avgTable.getItems().clear();
            ganttSRTF.getChildren().clear();
            ganttPriority.getChildren().clear();
            resLabel.setText("Best Algorithm: -");
        });

                btnA.setOnAction(e -> loadScenario(inputTable, new int[][]{{1,0,5,3}, {2,1,3,1}, {3,2,8,4}, {4,3,2,2}}));
        btnB.setOnAction(e -> loadScenario(inputTable, new int[][]{{1,0,10,1}, {2,1,2,5}, {3,2,2,5}})); 
        btnC.setOnAction(e -> loadScenario(inputTable, new int[][]{{1,0,20,1}, {2,1,1,5}, {3,2,1,5}, {4,3,1,5}}));
        btnD.setOnAction(e -> {
            pidF.setText("-1"); arrF.setText("0"); burF.setText("5"); priF.setText("1");
            addBtn.fire();
        });

        runBtn.setOnAction(e -> {
            if (processes.isEmpty()) return;
            calculateSRTF();
            renderOutput(tableSRTF, ganttSRTF);
            double sWT = calculateAvgWT(tableSRTF), sTAT = calculateAvgTAT(tableSRTF), sRT = calculateAvgRT(tableSRTF);

            calculatePriority();
            renderOutput(tablePriority, ganttPriority);
            double pWT = calculateAvgWT(tablePriority), pTAT = calculateAvgTAT(tablePriority), pRT = calculateAvgRT(tablePriority);

            avgTable.getItems().clear();
            avgTable.getItems().add(new AvgRow("SRTF", sWT, sTAT, sRT));
            avgTable.getItems().add(new AvgRow("Priority", pWT, pTAT, pRT));
            if (sWT < pWT)
               resLabel.setText("Best (Efficiency): SRTF");
            else if (pWT < sWT)
               resLabel.setText("Best (Efficiency): Priority");
            else
            resLabel.setText("Best (Efficiency): Tie");
        });

        stage.setScene(new Scene(root, 1250, 800));
        stage.setTitle("CPU Scheduler Comparison Tool - Project Edition");
        stage.show();
    }

    private void validateAndAdd(TextField pidF, TextField arrF, TextField burF, TextField priF, TableView<Process> table) throws Exception {
        if (pidF.getText().isEmpty() || arrF.getText().isEmpty() || burF.getText().isEmpty() || priF.getText().isEmpty()) 
            throw new Exception("Please fill all fields!");

        int pid = Integer.parseInt(pidF.getText());
        int arrival = Integer.parseInt(arrF.getText());
        int burst = Integer.parseInt(burF.getText());
        int priority = Integer.parseInt(priF.getText());

        if (pid < 0) throw new Exception("PID must be Positive Value");
        if (arrival < 0) throw new Exception("Arrival time cannot be negative");
        if (burst <= 0) throw new Exception("Burst time must be greater than 0");
        if (priority <= 0) throw new Exception("Priority must be greater than 0");

        for (Process p : processes) if (p.getPid() == pid) throw new Exception("Duplicate PID!");

        Process p = new Process(pid, arrival, burst, priority);
        processes.add(p);
        table.getItems().add(p);
        pidF.clear(); arrF.clear(); burF.clear(); priF.clear();
    }

    private void loadScenario(TableView<Process> table, int[][] data) {
        processes.clear();
        table.getItems().clear();
        for (int[] d : data) {
            Process p = new Process(d[0], d[1], d[2], d[3]);
            processes.add(p);
            table.getItems().add(p);
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

   
    private void calculateSRTF() {
        currentGantt.clear();
        int n = processes.size();
        finishTime = new int[n]; responseTime = new int[n];
        int[] rem = new int[n]; boolean[] done = new boolean[n];
        for (int i = 0; i < n; i++) { rem[i] = processes.get(i).burst; responseTime[i] = -1; }

        int time = 0, completed = 0, currentIdx = -1, start = 0;
        while (completed < n) {
            int bestIdx = -1, minVal = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (processes.get(i).arrival <= time && !done[i] && rem[i] < minVal) {
                    minVal = rem[i]; bestIdx = i;
                }
            }
            if (bestIdx != currentIdx) {
                if (time > start) currentGantt.add(new GanttEntry(currentIdx == -1 ? -1 : processes.get(currentIdx).pid, start, time));
                start = time; currentIdx = bestIdx;
                if (currentIdx != -1 && responseTime[currentIdx] == -1) responseTime[currentIdx] = time;
            }
            if (currentIdx == -1) time++;
            else {
                rem[currentIdx]--; time++;
                if (rem[currentIdx] == 0) { done[currentIdx] = true; finishTime[currentIdx] = time; completed++; }
            }
        }
        currentGantt.add(new GanttEntry(currentIdx == -1 ? -1 : processes.get(currentIdx).pid, start, time));
    }

    private void calculatePriority() {
    currentGantt.clear();
    int n = processes.size();
    finishTime = new int[n]; responseTime = new int[n];
    int[] rem = new int[n]; boolean[] done = new boolean[n];
    int[] waitCounter = new int[n]; 

    for (int i = 0; i < n; i++) { 
        rem[i] = processes.get(i).burst; 
        responseTime[i] = -1; 
        waitCounter[i] = 0;
    }

    int time = 0, completed = 0, currentIdx = -1, start = 0;
    while (completed < n) {
        int bestIdx = -1, minPri = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (processes.get(i).arrival <= time && !done[i]) {
                if (processes.get(i).priority < minPri) { 
                    minPri = processes.get(i).priority; 
                    bestIdx = i; 
                }
                else if (processes.get(i).priority == minPri) {
                    if (currentIdx != -1 && i == currentIdx) bestIdx = i;
                    else if (bestIdx == -1 || processes.get(i).arrival < processes.get(bestIdx).arrival) bestIdx = i;
                }
            }
        }

        for (int i = 0; i < n; i++) {
            if (processes.get(i).arrival <= time && !done[i] && i != bestIdx) {
                waitCounter[i]++;
                if (waitCounter[i] >= 5) { 
                    if (processes.get(i).priority > 1) {
                        processes.get(i).priority--;
                    }
                    waitCounter[i] = 0;
                }
            }
        }

        if (bestIdx != currentIdx) {
            if (time > start) currentGantt.add(new GanttEntry(currentIdx == -1 ? -1 : processes.get(currentIdx).pid, start, time));
            start = time; currentIdx = bestIdx;
            if (currentIdx != -1 && responseTime[currentIdx] == -1) responseTime[currentIdx] = time;
        }

        if (currentIdx == -1) time++;
        else {
            rem[currentIdx]--; time++;
            if (rem[currentIdx] == 0) { done[currentIdx] = true; finishTime[currentIdx] = time; completed++; }
        }
    }
    currentGantt.add(new GanttEntry(currentIdx == -1 ? -1 : processes.get(currentIdx).pid, start, time));
}

    private void renderOutput(TableView<ProcessResult> t, HBox g) {
        t.getItems().clear(); g.getChildren().clear();
        ArrayList<ProcessResult> results = new ArrayList<>();
        for (int i = 0; i < processes.size(); i++) {
            int tat = finishTime[i] - processes.get(i).arrival;
            int wt = tat - processes.get(i).burst;
            int rt = responseTime[i] - processes.get(i).arrival;
            results.add(new ProcessResult(processes.get(i).pid, wt, tat, rt, responseTime[i]));
        }
        results.sort(Comparator.comparingInt(ProcessResult::getPid));
        t.getItems().addAll(results);

        for (GanttEntry e : currentGantt) {
            String text = (e.pid == -1) ? "Idle" : "P" + e.pid;
            VBox box = new VBox(new Label(text), new Label(e.start + "-" + e.end));
            box.setPadding(new Insets(5)); box.setMinWidth(50);
            box.setStyle("-fx-border-color: black; -fx-background-color: " + (e.pid == -1 ? "#eee" : "#ddd") + "; -fx-alignment: center;");
            g.getChildren().add(box);
        }
    }

    private void setupInputTable(TableView<Process> t) {
        TableColumn<Process, Integer> c1 = new TableColumn<>("PID"); c1.setCellValueFactory(new PropertyValueFactory<>("pid"));
        TableColumn<Process, Integer> c2 = new TableColumn<>("Arrival"); c2.setCellValueFactory(new PropertyValueFactory<>("arrival"));
        TableColumn<Process, Integer> c3 = new TableColumn<>("Burst"); c3.setCellValueFactory(new PropertyValueFactory<>("burst"));
        TableColumn<Process, Integer> c4 = new TableColumn<>("Priority"); c4.setCellValueFactory(new PropertyValueFactory<>("priority"));
        t.getColumns().addAll(c1, c2, c3, c4); t.setPrefHeight(150);
    }

    private void setupResultTable(TableView<ProcessResult> t) {
        TableColumn<ProcessResult, Integer> c1 = new TableColumn<>("PID"); c1.setCellValueFactory(new PropertyValueFactory<>("pid"));
        TableColumn<ProcessResult, Integer> c2 = new TableColumn<>("WT"); c2.setCellValueFactory(new PropertyValueFactory<>("wt"));
        TableColumn<ProcessResult, Integer> c3 = new TableColumn<>("TAT"); c3.setCellValueFactory(new PropertyValueFactory<>("tat"));
        TableColumn<ProcessResult, Integer> c4 = new TableColumn<>("RT"); c4.setCellValueFactory(new PropertyValueFactory<>("rt"));
        t.getColumns().addAll(c1, c2, c3, c4); t.setPrefHeight(180);
    }

    private void setupAvgTable(TableView<AvgRow> t) {
        TableColumn<AvgRow, String> a1 = new TableColumn<>("Algorithm"); a1.setCellValueFactory(new PropertyValueFactory<>("algo"));
        TableColumn<AvgRow, Double> a2 = new TableColumn<>("Avg WT"); a2.setCellValueFactory(new PropertyValueFactory<>("wt"));
        TableColumn<AvgRow, Double> a3 = new TableColumn<>("Avg TAT"); a3.setCellValueFactory(new PropertyValueFactory<>("tat"));
        TableColumn<AvgRow, Double> a4 = new TableColumn<>("Avg RT"); a4.setCellValueFactory(new PropertyValueFactory<>("rt"));
        t.getColumns().addAll(a1, a2, a3, a4); t.setPrefHeight(100);
    }

    private double calculateAvgWT(TableView<ProcessResult> t) { return t.getItems().stream().mapToDouble(ProcessResult::getWt).average().orElse(0); }
    private double calculateAvgTAT(TableView<ProcessResult> t) { return t.getItems().stream().mapToDouble(ProcessResult::getTat).average().orElse(0); }
    private double calculateAvgRT(TableView<ProcessResult> t) { return t.getItems().stream().mapToDouble(ProcessResult::getRt).average().orElse(0); }

    public static void main(String[] args) { launch(args); }
}