package scripts;

import org.powerbot.script.*;
import org.powerbot.script.Random;
import org.powerbot.script.rt4.ClientContext;

import javax.swing.*;
import java.awt.*;


@Script.Manifest(name = "Banana Basketing", description = "Make cash from making banana baskets", properties = "client=4; topic=0;")
public class BananaBasketing extends PollingScript<ClientContext>  implements PaintListener
{
    //ToDo: Fix I Cant Reach That, Fix clicking on portal and going out of my house by mistake
    //Main Variables
    private enum State { FillBaskets, Bank };
    private int Profit = 0;

    //GUI Variables
    private Font font1 = new Font("Verdana", 0, 12);
   // private Image background = downloadImage("http://s24.postimg.org/yev4jy2it/AIOCon.png");
    private String CurrentTask = "Starting Up...";
    private long startTime;

    @Override
    public void start()
    {
        startTime = System.currentTimeMillis();
    }

    @Override
    public void poll()
    {
       switch (GetState()) {
            case FillBaskets:
                if (ctx.players.local().animation() == -1 && !(ctx.players.local().inMotion())) {
                    UpdateTask("Filling Basket");
                    ctx.inventory.select().id(new int[] {5376}).poll().click();
                    Condition.sleep(Random.nextInt(500, 1000));
                }
                break;
            case Bank:
                UpdateTask("Banking");
                if (!ctx.bank.opened()) ctx.bank.open();
                else if (ctx.inventory.select().count() != 28) {
                    Profit += 788;
                    ctx.bank.depositInventory();
                    CheckAndWithdraw(5376, 4, 4);
                    CheckAndWithdraw(1963, 0, 24);
                    Condition.sleep(Random.nextInt(300, (Random.nextInt(0, 48) == 8 ? 30000 : 700)));
                    ctx.bank.close();
                }
                break;
            }
    }

    private State GetState() {
        return ctx.inventory.select().id(new int[] {5376}).count() == 0 ? State.Bank : State.FillBaskets;
    }

    @Override
    public void repaint(Graphics g1)
    {
        Graphics2D g = (Graphics2D)g1;
        g.setColor(Color.GREEN);
        g.setFont(font1);
      //  g.drawImage(background, 10, 50, null);
        g.drawString("Runtime: " + format(System.currentTimeMillis() - startTime), 120, 95);
        g.drawString("Profit: " + Profit / 1000 + "k", 120, 110);
        g.drawString("Task: " + CurrentTask, 120, 125);
        g.drawString("Version: 1.0", 120, 140);
        g.drawLine((int)ctx.input.getLocation().getX() - 5, (int)ctx.input.getLocation().getY() - 5, (int)ctx.input.getLocation().getX() + 5, (int)ctx.input.getLocation().getY() + 5);
        g.drawLine((int)ctx.input.getLocation().getX() - 5, (int)ctx.input.getLocation().getY() + 5, (int)ctx.input.getLocation().getX() + 5, (int)ctx.input.getLocation().getY() - 5);
    }

    private void CheckAndWithdraw(int ItemId, int Amount, int AmountIShouldHave)
    {
        int count = ctx.inventory.select().id(new int[]{ItemId}).poll().stackSize();
        if (count < AmountIShouldHave)
        {
            if(ctx.bank.select().id(new int[]{ItemId}).isEmpty())
            {
                JOptionPane.showMessageDialog(null, "Not enough resources left in bank (1:" + ItemId+ ", "+Amount+", "+count, "Lacking Item", JOptionPane.INFORMATION_MESSAGE);
                ctx.controller.stop();
                return;
            } else if(ctx.bank.select().id(new int[]{ItemId}).poll().stackSize() < Amount)
            {
                JOptionPane.showMessageDialog(null, "Not enough resources left in bank (2:" + ItemId+ ", "+Amount+")", "Lacking Item", JOptionPane.INFORMATION_MESSAGE);
                ctx.controller.stop();
                return;
            }
            ctx.bank.withdraw(ItemId, Amount);
        }
    }

    private void UpdateTask(String Task)
    {
        CurrentTask = Task;
    }

    private String format(final long time) {
        final int sec = (int) (time / 1000), h = sec / 3600, m = sec / 60 % 60, s = sec % 60;
        return (h < 10 ? "0" + h : h) + ":" + (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s);
    }
}
