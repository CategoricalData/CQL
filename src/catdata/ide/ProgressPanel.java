package catdata.ide;

import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import catdata.RuntimeInterruptedException;
import catdata.Unit;

public class ProgressPanel extends JPanel implements ActionListener {

  private static final long serialVersionUID = 1L;
  
  private JProgressBar progressBar;
  
  public ProgressPanel(JComponent pan, JButton b, Function<Unit, JComponent> future) {
    super(new CardLayout());

    JPanel card1 = new JPanel();
    card1.add(pan);
    add(card1, "card1");
    
    progressBar = new JProgressBar();
    progressBar.setIndeterminate(false);
  
    JPanel card2 = new JPanel();
    card2.add(progressBar);
    add(card2, "card2");
    ProgressPanel c = this;
    b.addActionListener(x -> {
      ((CardLayout) getLayout()).show(c, "card2");
      Timer t = new Timer(1000, c);
      t.start();
      
      var task = new SwingWorker<JComponent, Void>() {

        @Override
        public JComponent doInBackground() {
          
          
          return future.apply(Unit.unit);
        }

        @Override
        public void done() {
          try {
            JComponent toAdd = get();
            t.stop();
            removeAll();
            setLayout(new GridLayout(1,1));
            add(toAdd);
            revalidate();
            repaint();
          } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
          } catch (ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        }
      };
      
         task.execute();
    });

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    int i = progressBar.getValue();
    i += 10;
    if (i > 100) {
      i = 0;
    }
    progressBar.setValue(i);
  }

}
