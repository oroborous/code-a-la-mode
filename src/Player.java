
import java.util.*;
import java.util.stream.Stream;

class Player {

    static final String DISH = "DISH";
    static final String BLUEBERRIES = "BLUEBERRIES";
    static final String ICE_CREAM = "ICE_CREAM";
    static final String STRAWBERRIES = "STRAWBERRIES";
    static final String CHOPPED_STRAWBERRIES = "CHOPPED_STRAWBERRIES";
    static final String DOUGH = "DOUGH";
    static final String CROISSANT = "CROISSANT";

    static Kitchen kitchen;
    static Oven oven;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        // ALL CUSTOMERS INPUT: to ignore until Bronze
        int numAllCustomers = in.nextInt();
        for (int i = 0; i < numAllCustomers; i++) {
            String customerItem = in.next();
            int customerAward = in.nextInt();
        }

        // KITCHEN INPUT
        kitchen = new Kitchen(in);

        // game loop
        while (true) {
            int turnsRemaining = in.nextInt();

            // PLAYERS INPUT
            Chef me = new Chef(in);
            System.err.println("Carrying: " + me.toString());

            Chef partner = new Chef(in);

            int numTablesWithItems = in.nextInt();
            Table[] tablesWithItems = new Table[numTablesWithItems];
            for (int i = 0; i < numTablesWithItems; i++) {
                tablesWithItems[i] = new Table();
                tablesWithItems[i].init(in);
            }

            // oven to ignore until bronze
            oven = new Oven(in);

            // CURRENT CUSTOMERS INPUT
            int numCustomers = in.nextInt();
            Customer[] customers = new Customer[numCustomers];
            for (int i = 0; i < numCustomers; i++) {
                customers[i] = new Customer(in);
            }

            // GAME LOGIC
            Table table = null;
            Customer easiestOrder = null;
            int leastMissingItems = Integer.MAX_VALUE;

            for (Customer customer : customers) {
                if (customer.matches(me.playerItems)) {
                    System.err.println("Matches customer: " + customer.toString());
                    // We have a complete dish
                    easiestOrder = customer;
                    table = kitchen.findClosestTable(me, TableType.WINDOW);
                } else if (customer.canMake(me.playerItems)) {
                    // Which order is missing the least items?
                    int numMissingItems = customer.getNumMissingItems(me.playerItems);

                    if (numMissingItems < leastMissingItems) {
                        // This order is easiest so far
                        leastMissingItems = numMissingItems;
                        easiestOrder = customer;
                        // System.err.println("Least items (" + leastMissingItems + ") :" + customer.toString());
                    } else if (numMissingItems == leastMissingItems) {
                        // This order is equally easy, but is it worth more completed?
                        if (easiestOrder == null || customer.customerAward > easiestOrder.customerAward) {
                            easiestOrder = customer;
                            // System.err.println("Biggest reward (" + leastMissingItems + ") :" + customer.toString());
                        }
                    }
                }

                if (easiestOrder == null) {
                    // What we're holding no good, go empty it in the dishwasher
                    System.err.println("No easiest order found");
                    table = kitchen.findClosestTrash(me);
                } else {
                    System.err.println("Working on: " + easiestOrder.toString());
                    if (table == null) {
                        List<TableType> tableTypes = easiestOrder.getTableTypesRequired(me.playerItems);
                        table = kitchen.findClosestTable(me, tableTypes);
                    }
                }
            }

            if (table != null) {
                table.use();
            } else {
                System.out.println("WAIT");
            }
        }
    }
}

class Oven {

    String ovenContents;
    int ovenTimer;

    public Oven(Scanner in) {
        ovenContents = in.next();
        ovenTimer = in.nextInt();
    }

    public boolean isEmpty() {
        return ovenContents.isEmpty();
    }

    public boolean contains(String item) {
        return ovenContents != null && ovenContents.equals(item);
    }
}

class Customer {

    String[] customerWants;
    int customerAward;

    public Customer(Scanner in) {
        String original = in.next();
        // System.err.println("Creating a customer: " + original);
        customerWants = original.split("-");
        customerAward = in.nextInt();
    }

    public boolean matches(String[] playerItems) {
        return getNumMissingItems(playerItems) == 0;
    }

    public boolean canMake(String[] playerItems) {
        // Can we fill this customer's order based on what we're currently holding?
        Set<String> holdingButNotInOrder = new HashSet<>(Arrays.asList(playerItems));
        Set<String> inOrder = new HashSet<>(Arrays.asList(customerWants));
        replacePartial(holdingButNotInOrder);

        holdingButNotInOrder.removeAll(inOrder);

        return holdingButNotInOrder.isEmpty();
    }

    private void replacePartial(Set<String> holding) {
        if (holding.contains(Player.STRAWBERRIES)) {
            holding.remove(Player.STRAWBERRIES);
            holding.add(Player.CHOPPED_STRAWBERRIES);
        }
        if (holding.contains(Player.DOUGH)) {
            holding.remove(Player.DOUGH);
            holding.add(Player.CROISSANT);
        }
    }

    public int getNumMissingItems(String[] playerItems) {
        if (playerItems == null) {
            return customerWants.length;
        }

        Set<String> missingFromOrder = new HashSet<>(Arrays.asList(customerWants));
        Set<String> playerHas = new HashSet<>(Arrays.asList(playerItems));
        missingFromOrder.removeAll(playerHas);

        return missingFromOrder.size();
    }

    public List<TableType> getTableTypesRequired(String[] playerItems) {
        List<TableType> destinations = new ArrayList<>();

        if (playerItems == null) {
            playerItems = new String[0];
        }

        Set<String> missingFromOrder = new HashSet<>(Arrays.asList(customerWants));
        Set<String> playerHas = new HashSet<>(Arrays.asList(playerItems));
        missingFromOrder.removeAll(playerHas);

        if (missingFromOrder.contains(Player.CROISSANT)) {
            if (Player.oven.contains(Player.CROISSANT)) {
                destinations.add(TableType.OVEN);
            } else if (Player.oven.isEmpty()) {
                if (playerHas.contains(Player.DOUGH)) {
                    destinations.add(TableType.OVEN);
                } else {
                    destinations.add(TableType.DOUGH);
                }
            }
        } else if (missingFromOrder.contains(Player.CHOPPED_STRAWBERRIES)) {
            if (playerHas.contains(Player.STRAWBERRIES)) {
                destinations.add(TableType.CHOPPING_BOARD);
            } else {
                destinations.add(TableType.STRAWBERRIES);
            }
        } else if (missingFromOrder.contains(Player.DISH)) {
            destinations.add(TableType.DISH);
        } else {
            missingFromOrder.stream().forEach((t) -> {
                destinations.add(TableType.get(t.charAt(0)));
            });
        }

        return destinations;
    }

    @Override
    public String toString() {
        return this.customerAward + " for [" + String.join(",", this.customerWants) + "]";
    }
}

class Chef {

    Position currentPosition, lastPosition;
    String playerItems[];

    public Chef(Scanner in) {
        int playerX = in.nextInt();
        int playerY = in.nextInt();

        lastPosition = currentPosition;
        currentPosition = new Position(playerX, playerY);

        String original = in.next();
        //System.err.println("Creating a chef: " + original);
        playerItems = original.equals("NONE") ? new String[0] : original.split("-");
    }

    public boolean isStuck() {
        return lastPosition != null && currentPosition.equals(lastPosition);
    }

    @Override
    public String toString() {
        return "[" + String.join(",", playerItems) + "]";
    }

}

class Kitchen {

    List<Table> tables;

    public Kitchen(Scanner in) {
        tables = new ArrayList<>();

        in.nextLine();
        for (int i = 0; i < 7; i++) {
            String kitchenLine = in.nextLine();
            for (int j = 0; j < kitchenLine.length(); j++) {
                char c = kitchenLine.charAt(j);
                TableType t = TableType.get(c);
                if (t != null) {
                    tables.add(new Table(t, j, i));
                }
            }
            System.err.println(kitchenLine);
        }
    }

    public Table findClosestTrash(Chef chef) {
        boolean hasDish = Stream.of(chef.playerItems).anyMatch((t) -> {
            return t.equals(Player.DISH);
        });

        return findClosestTable(chef, hasDish ? TableType.DISH : TableType.EMPTY);
    }

    public Table findClosestTable(Chef chef, TableType tableType) {
        return tables.stream().filter(t -> tableType == t.type)
                .min((t1, t2) -> {
                    return t1.distanceFrom(chef.currentPosition).compareTo(t2.distanceFrom(chef.currentPosition));
                }).get();
    }

    public Table findClosestTable(Chef chef, List<TableType> tableTypes) {
        return tables.stream().filter(t -> tableTypes.contains(t.type))
                .min((t1, t2) -> {
                    return t1.distanceFrom(chef.currentPosition).compareTo(t2.distanceFrom(chef.currentPosition));
                }).get();
    }
}

class Position {

    int x, y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(Position p) {
        return this.x == p.x && this.y == p.y;
    }

    public double distance(Position p) {
        if (p == null) {
            return Double.MAX_VALUE;
        }
        // Distance formula
        return Math.sqrt(Math.pow(p.x - this.x, 2) + Math.pow(p.y - this.y, 2));
    }
}

class Table {

    TableType type;

    Position position;

    public Table(TableType t, int x, int y) {
        type = t;
        position = new Position(x, y);
    }

    public Table() {
        this(TableType.EMPTY, 0, 0);
    }

    public Double distanceFrom(Position p) {
        return position.distance(p);
    }

    public double distanceFrom(Table table) {
        return table == null ? Double.MAX_VALUE : position.distance(table.position);
    }

    public void init(Scanner in) {
        int x = in.nextInt();
        int y = in.nextInt();
        this.position = new Position(x, y);
        this.type = TableType.get(in.next().charAt(0));
    }

    public void error() {
        System.err.println("Table of type " + type + " at " + position.x + " " + position.y);
    }

    public boolean matches(Character c) {
        return type == TableType.get(c);
    }

    public boolean matches(TableType tableType) {
        return type == tableType;
    }

    public void use() {
        System.out.println("USE " + position.x + " " + position.y + "; Stacy's AI");
    }
}

enum TableType {
    DOUGH,
    OVEN,
    BLUEBERRIES,
    ICE_CREAM,
    DISH,
    STRAWBERRIES,
    CHOPPING_BOARD,
    EMPTY,
    WINDOW;

    static TableType get(Character c) {
        switch (c) {
            case 'H':
                return DOUGH;
            case 'O':
                return OVEN;
            case 'C':
                return CHOPPING_BOARD;
            case 'D':
                return DISH;
            case 'S':
                return STRAWBERRIES;
            case 'B':
                return BLUEBERRIES;
            case 'I':
                return ICE_CREAM;
            case 'W':
                return WINDOW;
            case '#':
                return EMPTY;

        }
        return null;
    }
}
