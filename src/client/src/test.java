public class test {

    public static void main(String[] args) {
        SCClient client1 = new SCClient("user1", null);
        try {
            Thread.sleep(100);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        SCClient client2 = new SCClient("user2", null);
        if(client1.isInitialized() && client2.isInitialized()) {
            System.out.println("Clients inited");
        } else {
            System.out.println("Failed to init clients");
        }
    }

}
