package scripts;

import org.powerbot.script.*;
import org.powerbot.script.Random;
import org.powerbot.script.rt4.*;
import org.powerbot.script.rt4.ClientContext;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


@Script.Manifest(name = "AIO Construction", description = "Simply train construction xp no level requirements", properties = "client=4; topic=0;")
public class ConstructionLeveler extends PollingScript<ClientContext>  implements PaintListener
{
    //Main Variables
    private enum State { Build, Bank, WalkBank };
    private int PlanksID = 960, NailsId = 4819, CostToBuild = 4, ObjectToBuild = 15439, ObjectToRemove = 6791, BuildFails = 0, BuildIndex = 1;
    private boolean OptiosnOpen = true, BuildModeEnabled = true, Built = false;
    private Tile RemovedAt;
    private GameObject BuildObj;
    private Area Falador = new Area(new Tile(2943, 3363), new Tile(2981, 3392));

    //JSwing
    private JFrame frame = new JFrame("Configurations");
    private JButton Next;
    private JLabel jcomp2, jcomp3, jcomp6;
    private JList jcomp4;
    private DefaultListModel model = new DefaultListModel();
    private JButton jcomp5;
    private JCheckBox TeleTabs;
    private final JComboBox<Planks> planks = new JComboBox<Planks>(Planks.values());
    private final JComboBox<Nails> nails = new JComboBox<Nails>(Nails.values());
    private final JComboBox<String> BuildOff = new JComboBox<String>(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20"});

    //GUI Variables
    private int Crafted = 0;
    private Font font1 = new Font("Verdana", 0, 12);
    private Image background = downloadImage("http://s24.postimg.org/yev4jy2it/AIOCon.png");
    private String CurrentTask = "Starting Up...";
    private long startTime;

    //Pathing Variables
    private boolean PathBuilt = false;
    private Tile[] PATH = {};
    private TilePath FollowPath;
    private int[][][] PathToBank = {{{2956, 2959}, {3380, 3383}}, {{2950, 2952}, {3372, 3376}}, {{2945, 2947}, {3368, 3369}}};

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
        if(!OptiosnOpen)
        switch (GetState())
        {
            case Build:
                if (ctx.players.local().animation() == -1 && !(ctx.players.local().inMotion())) {
                    if(!BuildModeEnabled) {
                        BuildMode();
                    }
                    BuildObj = ctx.objects.select(Built ? 1 : 20).id(new int[]{ObjectToBuild}).nearest().poll();
                    if (BuildObj.valid()) {
                        if(Built) {
                            Condition.sleep(Random.nextInt(400, 1500));
                            if(BuildFails++ > 4) {
                                Built = false;
                                BuildFails = 0;
                            }
                            return;
                        }
                        AntiBan();
                        UpdateTask("Building");
                        if(ctx.players.local().tile().distanceTo(BuildObj.tile()) > 1) {
                            ctx.movement.step(BuildObj.tile());
                            return;
                        }
                        if (!BuildObj.inViewport()) ctx.camera.turnTo(BuildObj.tile());
                        Condition.sleep(Random.nextInt(400, 1000));
                        BuildObj.interact("Build");
                        Condition.sleep(Random.nextInt(1000, 2000));
                        if(ctx.widgets.widget(458).valid()) {
                            ctx.widgets.widget(458).component(3 + BuildIndex).click();
                            String tx[] = ctx.widgets.widget(458).component(3 + BuildIndex).component(3).text().split("lank: ");
                            if(tx.length > 1) CostToBuild = Integer.parseInt(tx[1].split("<")[0]);
                            Built = true;
                        }
                        Condition.sleep(Random.nextInt(4000, 6000));
                        Crafted++;
                    }
                    else
                    {
                        UpdateTask("Removing");
                        BuildObj = ctx.objects.select(Built ? 1 : 20).id(new int[]{ObjectToRemove}).nearest().poll();
                        if (BuildObj.valid()) {
                            if(ctx.players.local().tile().distanceTo(BuildObj.tile()) > 1) {
                                ctx.movement.step(BuildObj.tile());
                                return;
                            }
                            if (!BuildObj.inViewport()) ctx.camera.turnTo(BuildObj.tile());
                            RemovedAt = ctx.players.local().tile();
                            Condition.sleep(Random.nextInt(400, 800));
                            BuildObj.interact("Remove");
                            Condition.sleep(Random.nextInt(400, 800));

                            //Fix clicking Yes if the guy starts running when not close to object
                            if(RemovedAt.distanceTo(ctx.players.local().tile()) > 0) Condition.sleep(Random.nextInt(2000, 3900));

                            ctx.input.click(Random.nextInt(249, 269), Random.nextInt(472, 478), true);
                            BuildFails = 0;
                            Built = false;
                        }
                    }
                }
                break;
            case WalkBank:
                if (ctx.players.local().animation() == -1 && !(ctx.players.local().inMotion()))
                {
                    UpdateTask("Banking");
                    if (!PathBuilt) FollowPath = BuildPath(true);
                    Condition.sleep(Random.nextInt(100, 1000));
                    FollowPath.randomize(0, 0);
                    FollowPath.traverse();
                }
                break;
            case Bank:
                PathBuilt = false;
                if (!ctx.bank.opened()) ctx.bank.open();
                else if (ctx.inventory.select().count() != 28)
                {
                    CheckAndWithdraw(PlanksID, 24, 24);
                    CheckAndWithdraw(NailsId, 600, 40);
                    CheckAndWithdraw(563, 100, 2);
                    CheckAndWithdraw(557, 600, 2);
                    CheckAndWithdraw(8794, 1, 1);
                    CheckAndWithdraw(2347, 1, 1);
                    UpdateTask("AntiBan - Sleeping");
                    Condition.sleep(Random.nextInt(500,  (Random.nextInt(0, 16) == 8 ? 30000 : 2000)));
                }
                else
                {
                    ctx.bank.close();
                    Teleport(Magic.Spell.TELEPORT_TO_HOUSE);
                }
                break;
        }
    }

    @Override
    public void repaint(Graphics g1)
    {
        Graphics2D g = (Graphics2D)g1;
        g.setColor(Color.GREEN);
        g.setFont(font1);
        g.drawImage(background, 10, 50, null);
        g.drawString("Runtime: " + format(System.currentTimeMillis() - startTime), 120, 95);
        g.drawString("Built: " + Crafted, 120, 110);
        g.drawString("Task: " + CurrentTask, 120, 125);
        g.drawString("Version: 1.0", 120, 140);
        g.drawLine((int)ctx.input.getLocation().getX() - 5, (int)ctx.input.getLocation().getY() - 5, (int)ctx.input.getLocation().getX() + 5, (int)ctx.input.getLocation().getY() + 5);
        g.drawLine((int)ctx.input.getLocation().getX() - 5, (int)ctx.input.getLocation().getY() + 5, (int)ctx.input.getLocation().getX() + 5, (int)ctx.input.getLocation().getY() - 5);
    }

    private State GetState()
    {
        if(Falador.contains(ctx.players.local().tile()))
        {
            if (ctx.bank.opened()) return State.Bank;
            else if (ctx.bank.nearest().tile().distanceTo(ctx.players.local().tile()) < 3 && ctx.bank.inViewport()) return State.Bank;
            else if (NeedsBank()) return State.WalkBank;
        }
        else if(ctx.inventory.select().id(new int[] { PlanksID }).count() < CostToBuild)
        {
            Teleport(Magic.Spell.FALADOR_TELEPORT);
            return State.WalkBank;
        }
        return State.Build;
    }

    private void CheckAndWithdraw(int ItemId, int Amount, int AmountIShouldHave)
    {
        int count = ctx.inventory.select().id(new int[]{ItemId}).poll().stackSize();
        if (count < AmountIShouldHave)
        {
            if(ctx.bank.select().id(new int[]{ItemId}).isEmpty())
            {
                JOptionPane.showMessageDialog(null, "Not enough resources left in bank (1:" + ItemId+ ", "+Amount+")", "Lacking Item", JOptionPane.INFORMATION_MESSAGE);
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

    private boolean NeedsBank()
    {
        return ctx.inventory.select().count() < 28 || ctx.inventory.select().id(new int[]{NailsId}).poll().stackSize() < CostToBuild;
    }

    private JPanel CreateConfig()
    {
        JPanel Pan = new JPanel();

        Next = new JButton ("Continue");
        jcomp2 = new JLabel ("Planks:");
        jcomp3 = new JLabel ("Nails:");
        jcomp6 = new JLabel ("Build:");
       // TeleTabs = new JCheckBox ("TeleTabs");

        Pan.setPreferredSize (new Dimension (266, 131));
        Pan.setLayout (null);

        nails.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { NailsId = ((Nails)(nails.getSelectedItem())).getId(); } });
        planks.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { PlanksID = ((Planks)(planks.getSelectedItem())).getId(); } });
        Next.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { SetPage(SelectObjects(true)); } });
        BuildOff.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { BuildIndex = BuildOff.getSelectedIndex() + 1; } });

        Pan.add(Next);
        Pan.add(jcomp2);
        Pan.add(jcomp3);;
        Pan.add(jcomp6);
        Pan.add(planks);
        Pan.add(nails);
        Pan.add(BuildOff);
       // Pan.add(TeleTabs);

        Next.setBounds (0, 96, 266, 35);
        jcomp2.setBounds (65, 0, 100, 25);
        jcomp3.setBounds (75, 30, 100, 25);
        jcomp6.setBounds (75, 60, 100, 25);
        planks.setBounds (120, 0, 100, 25);
        nails.setBounds (120, 30, 100, 25);
        BuildOff.setBounds (120, 60, 100, 25);
      //  TeleTabs.setBounds (70, 60, 100, 25);
        return Pan;
    }

    private JPanel SelectObjects(final boolean ToBuild)
    {
        Next = new JButton ("Continue");
        jcomp2 = new JLabel ((ToBuild ? "Simply walk next to the unbuilt object and" : "Now simply pause the script and build the object"));
        jcomp3 = new JLabel ((ToBuild ? "Press refresh and select it in the list" : "Then Resume the script and press Refresh"));
        jcomp5 = new JButton ("Refresh");

        JPanel Pan = new JPanel();
        jcomp4 = new JList(model);
        JScrollPane pane = new JScrollPane(jcomp4);

        Pan.setPreferredSize (new Dimension (263, 287));
        Pan.setLayout (null);

        Next.addActionListener(new ActionListener() {   public void actionPerformed(ActionEvent e) {
            if(jcomp4.getSelectedValue() == null)
            {
                JOptionPane.showMessageDialog(null, "Please select a object first", "Object Not Selected", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if(ToBuild)
            {
                jcomp4.clearSelection();
                model.clear();
                jcomp4.setModel(model);
                SetPage(SelectObjects(false));
            }
            else if(ObjectToBuild != ObjectToRemove) {
                frame.dispose();
                OptiosnOpen = false;
            } else
            {
                JOptionPane.showMessageDialog(null, "It seems you selected the same object", "Objects Are The Same", JOptionPane.INFORMATION_MESSAGE);
                SetPage(SelectObjects(true));
            }
        } });
        jcomp5.addActionListener(new ActionListener() {   public void actionPerformed(ActionEvent e) {
            jcomp4.clearSelection();
            model.clear();
            for(final GameObject go : ctx.objects.select(2).nearest())
                if(go != null && go.valid() && go.name() != "null" && go.name() != null && !go.name().contains("Nothing") && go.id() != 13098 && go.id() != 13099) {
                    model.addElement(new JObjecta(go.name() + "(" + go.id() + ")", go.id()));
                }
            jcomp4.setModel(model);
        } });
        jcomp4.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                if (evt == null || evt.getValueIsAdjusting() || ((JObjecta)jcomp4.getSelectedValue()) == null || jcomp4.getSelectedIndex() < 0) return;
                if (ToBuild) ObjectToBuild = ((JObjecta)jcomp4.getSelectedValue()).getId();
                else ObjectToRemove = ((JObjecta)jcomp4.getSelectedValue()).getId();
            }
        });

        Pan.add (Next);
        Pan.add (jcomp2);
        Pan.add (jcomp3);
        Pan.add (pane);
        Pan.add (jcomp5);

        Next.setBounds (0, 255, 266, 35);
        jcomp2.setBounds (10, 0, 260, 25);
        jcomp3.setBounds (25, 20, 260, 25);
        pane.setBounds (0, 45, 263, 175);
        jcomp5.setBounds (0, 220, 266, 35);
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
        Condition.sleep(Random.nextInt(3000, 4500));
        BuildModeEnabled = false;
        Built = true;
        if(S == Magic.Spell.TELEPORT_TO_HOUSE) BuildMode();
    }

    private void BuildMode()
    {
        CurrentTask = "Enabling Build Mode";
        Condition.sleep(Random.nextInt(1400, 2500));
        ctx.game.tab(Game.Tab.OPTIONS);
        Condition.sleep(Random.nextInt(500, 1200));
        if(ctx.widgets.widget(261).valid()) ctx.widgets.widget(261).component(70).click();
        Condition.sleep(Random.nextInt(500, 1200));
        if(ctx.widgets.widget(370).valid()) {
            BuildModeEnabled = true;
            ctx.widgets.widget(370).component(5).click();
        }
        Condition.sleep(Random.nextInt(3000, 5000));
        ctx.game.tab(Game.Tab.INVENTORY);
        Built = false;
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

    private TilePath BuildPath(boolean Bank)
    {
        PATH = new Tile[3];
        for(int i = 0; i < 3; i++) PATH[i] = new Tile(Random.nextInt((PathToBank)[i][0][0], PathToBank[i][0][1]), Random.nextInt(PathToBank[i][1][0], PathToBank[i][1][1]), 0);
        PathBuilt = true;
        return ctx.movement.newTilePath(PATH);
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
        Planks("Planks", 960),
        OakPlanks("Oak Planks", 8778),
        MagPlanks("Maghogany Planks", 8782),
        TeakPlanks("Teak Planks", 8780);

        private final String name;
        private final int id;
        Planks(final String name, final int id) {
            this.name = name;
            this.id = id;
        }
        public String getName() { return name; }
        public int getId() { return id; }
        public String toString() { return name; }
    }

    private enum Nails
    {
        Bronze("Bronze", 4819),
        Iron("Iron", 4820),
        Steel("Steel", 1539),
        Black("Black", 4821),
        Mithril("Mithril", 4822),
        Adamantite("Adamantite", 4823),
        Rune("Rune", 4824);

        private final String name;
        private final int id;
        Nails(final String name, final int id)
        {
            this.name = name;
            this.id = id;
        }
        public String getName() { return name; }
        public int getId() { return id; }
        public String toString() { return name; }
    }

    private class JObjecta
    {
        private String name;
        private int id;

        public JObjecta(String name, int id)
        {
            this.name = name;
            this.id = id;
        }

        public String getName() { return name; }
        public int getId() { return id; }
        public String toString() { return name; }
    }
}
