package scripts;

import org.powerbot.script.*;
import org.powerbot.script.Random;
import org.powerbot.script.rt4.*;
import org.powerbot.script.rt4.Bank;
import org.powerbot.script.rt4.ChatOption;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Equipment;
import org.powerbot.script.rt4.Game;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


@Script.Manifest(name = "Demon Planks", description = "Logs To Planks, 50 con and demon butler needed", properties = "client=4; topic=0;")
public class DemonPlanks extends PollingScript<ClientContext>  implements PaintListener
{
    //ToDo: Fix I Cant Reach That, Fix clicking on portal and going out of my house by mistake
    //Main Variables
    State CurrentState;
    private enum State { UseBitch, Bank, Chatting };
    private int WoodID = 1511, Profit = 0, FailCount = 0, ChatIndex = 0;
    private boolean OptiosnOpen = true, BitchCalled = false, PortalClicked = false;
    private Area Camalot = new Area(new Tile(2742, 3484), new Tile(2774, 3461));
    private Npc Bitch;
    //JSwing
    private JFrame frame = new JFrame("Configurations");
    private JButton Next;
    private JLabel jcomp2;
    private final JComboBox<Planks> planks = new JComboBox<Planks>(Planks.values());
    Planks SelectedLogs = Planks.Planks;
    //GUI Variables
    private Font font1 = new Font("Verdana", 0, 12);
    //private Image background = downloadImage("http://s24.postimg.org/yev4jy2it/AIOCon.png"); will add later
    private String CurrentTask = "Starting Up...";
    private long startTime;

    @Override
    public void start()
    {
        while(ctx.equipment.itemAt(Equipment.Slot.MAIN_HAND).id() != 1381)
        {
            JOptionPane.showMessageDialog(null, "No Staff of air, i suggest you read the instructions before trying this", "Wrong Equipment", JOptionPane.INFORMATION_MESSAGE);
            Condition.sleep(10000);
        }
        SetPage(CreateConfig());
        startTime = System.currentTimeMillis();
    }

    @Override
    public void poll()
    {
        if(!OptiosnOpen) {
            if(Camalot.contains(ctx.players.local().tile()))
            {
                if (ctx.bank.opened()) CurrentState = State.Bank;
                else if (ctx.bank.inViewport()) CurrentState = State.Bank;
                else ctx.camera.turnTo(ctx.bank.nearest());
            }
            else if(ctx.inventory.select().id(new int[] { WoodID }).count() < 24)
            {
                BitchCalled = false;
                Teleport(Magic.Spell.CAMELOT_TELEPORT);
                CurrentState = State.Bank;
            } else if(CurrentState != State.Chatting) CurrentState = State.UseBitch;

            switch (CurrentState) {
                case Chatting: HandleChatting(); break;
                case UseBitch:
                    if (ctx.players.local().animation() == -1 && !(ctx.players.local().inMotion())) {
                            CurrentTask = "Calling Demon Butler";
                            Condition.sleep(Random.nextInt(1400, 2500));
                            ctx.game.tab(Game.Tab.OPTIONS);
                            Condition.sleep(Random.nextInt(500, 1200));
                            if (ctx.widgets.widget(261).valid()) ctx.widgets.widget(261).component(70).click();
                            Condition.sleep(Random.nextInt(500, 1200));
                            if (ctx.widgets.widget(370).valid())
                            {
                                ctx.widgets.widget(370).component(15).click();
                                BitchCalled = true;
                                CurrentState = State.Chatting;
                            }
                            Condition.sleep(Random.nextInt(300, 1000));
                    }
                    break;
                case Bank:
                    UpdateTask("Banking");
                    if (!ctx.bank.opened()) ctx.bank.open();
                    else if (ctx.inventory.select().count() != 28) {
                        CheckAndWithdraw(563, 100, 2);
                        CheckAndWithdraw(555, 100, 2);
                        CheckAndWithdraw(557, 100, 2);
                        CheckAndWithdraw(995, 100000, SelectedLogs.cashneeded()+10000);
                        CheckAndWithdraw(WoodID, 0, 24);
                        Condition.sleep(Random.nextInt(500, (Random.nextInt(0, 16) == 8 ? 30000 : 2000)));
                    } else {
                        ctx.bank.close();
                        ChatIndex = 0;
                        Teleport(Magic.Spell.TELEPORT_TO_HOUSE);
                    }
                    break;
            }
        }
    }

    @Override
    public void repaint(Graphics g1)
    {
        Graphics2D g = (Graphics2D)g1;
        g.setColor(Color.GREEN);
        g.setFont(font1);
       /// g.drawImage(background, 10, 50, null);
        g.drawString("Runtime: " + format(System.currentTimeMillis() - startTime), 120, 95);
        g.drawString("Profit: " + Profit / 1000 + "k", 120, 110);
        g.drawString("Task: " + CurrentTask, 120, 125);
        g.drawString("Version: 1.0", 120, 140);
        g.drawLine((int)ctx.input.getLocation().getX() - 5, (int)ctx.input.getLocation().getY() - 5, (int)ctx.input.getLocation().getX() + 5, (int)ctx.input.getLocation().getY() + 5);
        g.drawLine((int)ctx.input.getLocation().getX() - 5, (int)ctx.input.getLocation().getY() + 5, (int)ctx.input.getLocation().getX() + 5, (int)ctx.input.getLocation().getY() - 5);
    }

    private boolean ContineChat(int NewIndex)
    {
        if (ctx.chat.canContinue())
        {
            ctx.chat.clickContinue(true);
            ChatIndex = NewIndex;
            FailCount = 0;
            return true;
        }
        return false;
    }

    private boolean UseBitch(int NewIndex)
    {
        Npc n = ctx.npcs.select().id(new int[]{229}).nearest().poll();
        if (n.valid()) {
            if (n.inViewport()) ctx.camera.turnTo(n);
            n.click();
            FailCount = 0;
            ChatIndex = NewIndex;
            return true;
        }
        return false;
    }

    private boolean ClickChat(String Option, int NewIndex)
    {
        ChatOption chat = ctx.chat.select().text(Option).poll();
        if (chat != null && chat.valid()) {
            chat.select(true);
            ChatIndex = NewIndex;
            FailCount = 0;
            return true;
        }
        return false;
    }

    private void HandleChatting()
    {
        Bitch = ctx.npcs.select().id(new int[]{229}).nearest().poll();
        if(ctx.npcs.select().id(new int[]{229}).isEmpty() || !Bitch.valid() || Bitch.tile().distanceTo(ctx.players.local().tile()) > 20)
        {
            if(!PortalClicked) {
                GameObject Portal = ctx.objects.select(5).id(new int[]{15480}).nearest().poll();
                if (Portal.valid()) {
                    if (Portal.inViewport()) ctx.camera.turnTo(Portal);
                    Portal.click();
                    PortalClicked = true;
                    Condition.sleep(Random.nextInt(600, 1300));
                }
            }
            else
            {
                ClickChat("Go to your house", 0);
                Condition.sleep(Random.nextInt(2000, 3000));
                CurrentState = State.UseBitch;
            }
        }
        else if(!BitchCalled || Bitch.tile().distanceTo(ctx.players.local().tile()) > 4)
        {
            CurrentState = State.UseBitch;
            return;
        }
        UpdateTask("Chatting To Butler: " + ChatIndex);
        switch (ChatIndex) {
            case 0:
                if(ClickChat("Take to sawmill", 1)) return;
                else if(ContineChat(4)) return;
                break;
            case 1: if(ContineChat(2)) return; break;
            case 2: if(ClickChat("Yes", 3)) return; break;
            case 3:
                if(ContineChat(0))
                {
                    Profit += SelectedLogs.profit();
                    return;
                }
                break;
            case 4:
                if(ClickChat("Pay servant", 5))
                {
                    Profit -= 10000;
                    return;
                }
                else if(ClickChat("Go to the saw", 7)) return;
                break;
            case 5: if(ContineChat(6)) return; break;
            case 6: if(UseBitch(0)) return; break;
            case 7: if(ContineChat(8)) return; break;
            case 8: {
                if(ctx.game.tab() != Game.Tab.INVENTORY) ctx.game.tab(Game.Tab.INVENTORY);
                ctx.inventory.select().id(new int[]{WoodID}).poll().click();
                ChatIndex = 9;
                FailCount = 0;
            }
            break;
            case 9: if(UseBitch(10)) return; break;
            case 10: if(ClickChat("Sawmill", 11)) return; break;
            case 11: {
                if(ctx.chat.pendingInput()) {
                    ctx.chat.sendInput(24);
                    ChatIndex = 1;
                    FailCount = 0;

                }
            }
            break;
        }
        if(FailCount++ > 5) {
            UseBitch(0);
            Condition.sleep(Random.nextInt(500, 1000));
        }
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

    private JPanel CreateConfig()
    {
        JPanel Pan = new JPanel();

        Next = new JButton ("Continue");
        jcomp2 = new JLabel ("Logs:");

        Pan.setPreferredSize (new Dimension (266, 131));
        Pan.setLayout (null);

        planks.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { SelectedLogs = ((Planks)(planks.getSelectedItem())); WoodID = ((Planks)(planks.getSelectedItem())).getId(); log.info("WoodId: "  + WoodID);} });
        Next.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { frame.dispose(); OptiosnOpen = false; } });

        Pan.add(Next);
        Pan.add(jcomp2);
        Pan.add(planks);
        Next.setBounds (0, 96, 266, 35);
        jcomp2.setBounds (65, 0, 100, 25);
        planks.setBounds (120, 0, 100, 25);
        return Pan;
    }

    private void SetPage(JPanel p)
    {
        frame.getContentPane().removeAll();
        frame.getContentPane().add(p);
        frame.pack();
        frame.setVisible (true);
    }

    private void Teleport(Magic.Spell S)
    {
        CurrentTask = "Teleporting";
        if(ctx.game.tab() != Game.Tab.MAGIC)
        {
            ctx.game.tab(Game.Tab.MAGIC);
            Condition.sleep(Random.nextInt(500, 1000));
        }
        ctx.magic.cast(S);
        Condition.sleep(Random.nextInt(2000, 3000));
    }

    private void AntiBan()
    {
        if (Random.nextInt(0, 8) == 4)
        {
            CurrentTask = "AntiBan - Sleeping";
            Condition.sleep(Random.nextInt(1000, 5000));
        }
        else if (Random.nextInt(0, 32) == 16)
        {
            CurrentTask = "AntiBan - Sleeping";
            Condition.sleep(Random.nextInt(10000, 40000));
        }
        if(Random.nextInt(0, 16) == 8)
        {
            CurrentTask = "AntiBan - Stats";
            if(ctx.game.tab() != Game.Tab.STATS) ctx.game.tab(Game.Tab.STATS);
            if(Random.nextInt(0, 2) > 0) ctx.game.tab(Game.Tab.INVENTORY);
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

    private enum Planks
    {
        Planks("Wood", 1511, 3686, 2400),
        OakPlanks("Oak", 1521, 	3662, 6000),
        TeakPlanks("Teak", 6333, 4284, 12000),
        MagPlanks("Maghogany", 6332, 4960, 39000);

        private final String name;
        private final int id, profit, cashneeded;
        Planks(final String name, final int id, final int profit, final int cashneeded) {
            this.name = name;
            this.id = id;
            this.profit = profit;
            this.cashneeded = cashneeded;
        }
        public String getName() { return name; }
        public int getId() { return id; }
        public String toString() { return name; }
        public int profit() { return profit; }
        public int cashneeded() { return cashneeded; }
    }
}
