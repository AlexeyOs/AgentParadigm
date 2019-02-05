package osetsky;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 @author Giovanni Caire - TILAB
 */
class GrantSellerGui extends JFrame {
    private GrantSellerAgent myAgent;

    private JTextField titleField, priceField, periodField;

    GrantSellerGui(GrantSellerAgent a) {
        super(a.getLocalName());

        myAgent = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(3, 2));
        p.add(new JLabel("Grant title:"));
        titleField = new JTextField(15);
        p.add(titleField);
        p.add(new JLabel("Price:"));
        priceField = new JTextField(15);
        p.add(priceField);
        p.add(new JLabel("Period of execution in count months:"));
        periodField = new JTextField(16);
        p.add(periodField);
        getContentPane().add(p, BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        addButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String title = titleField.getText().trim();
                    String price = priceField.getText().trim();
                    String period = periodField.getText().trim();
                    myAgent.updateCatalogue(title, Integer.parseInt(price),Integer.parseInt(period));
                    titleField.setText("");
                    priceField.setText("");
                    periodField.setText("");
                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(GrantSellerGui.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } );
        p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        // Make the agent terminate when the user closes
        // the GUI using the button on the upper right corner
        addWindowListener(new	WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        } );

        setResizable(false);
    }

    public void show() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.show();
    }
}
