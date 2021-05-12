package io;

import org.apache.batik.swing.JSVGCanvas;

import javax.swing.*;

public class MainScreen extends JFrame{
    private JPanel mainPanel;
    private JSVGCanvas JSVGCanvas1;
    private JCheckBox showAnimationCheckBox;
    private JCheckBox showSourceCheckBox;
    private JCheckBox showTargetCheckBox;
    private JButton runButton;
    private JComboBox comboBox1;
    private JButton loadButton1;
    private JButton loadButton2;
    private JButton sButton;
    private JButton loadButton;
    private JCheckBox a2FilesCheckBox;
    private JRadioButton radioButton1;


    MainScreen(){
        super("Main window");
        this.setContentPane(this.mainPanel);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        System.out.println("lalala");
        this.pack();
    }

    public static void main(String[] args){
        MainScreen ms = new MainScreen();
        ms.setVisible(true);

    }
}
