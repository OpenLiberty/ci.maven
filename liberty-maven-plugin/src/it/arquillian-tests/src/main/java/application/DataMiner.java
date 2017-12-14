package application;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import application.transaction.Transaction;

@Singleton
public class DataMiner {

    private static final int ONE_MINUTE = 60000;

    @PersistenceContext(unitName = "pu")
    private EntityManager em;

    @Schedule(hour = "*", minute = "*", second = "*/10", persistent = false)
    public void fetchData() {
        System.out.println("here");

        long currentTime = System.currentTimeMillis();
        long startTime = (currentTime - ONE_MINUTE) / 1000;

        Client c = ClientBuilder.newClient();
        JsonArray arr = c.target("https://api.gdax.com/products/BTC-USD/trades").request().get(JsonArray.class);

        float totalBuyAmount = 0;
        float totalSellAmount = 0;
        float totalValue = 0;

        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.getJsonObject(i);

            long time = 0;
            double size = 0;
            double price = 0;
            String side = null;

            for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonValue val = entry.getValue();
                // System.out.println("key: " + key + ", value: " + val);
                switch (key) {
                case "time":
                    String timestamp = ((JsonString) val).getString();
                    time = convertTimestampToEpoch(timestamp);
                    break;
                case "size":
                    size = Double.parseDouble(((JsonString) val).getString());
                case "price":
                    price = Double.parseDouble(((JsonString) val).getString());
                    break;
                case "side":
                    side = ((JsonString) val).getString();
                    break;
                }
            }

            if ("".equals(side) || time == -1 || time < (startTime * 1000)) {
                // System.out.println("skipping");
                continue;
            }

            double value = size * price;
            if ("buy".equals(side)) {
                totalBuyAmount += size;
            } else {
                totalSellAmount += size;
            }
            totalValue += value;
        }

        float sharePrice = 0;
        if (totalBuyAmount > 0 || totalSellAmount > 0) {
            sharePrice = totalValue / (totalBuyAmount + totalSellAmount);
        }

        if (sharePrice > 0) {
            Transaction t = new Transaction();
            t.setTime(currentTime);
            t.setPrice(sharePrice);
            t.setBuy(totalBuyAmount);
            t.setSell(totalSellAmount);
            em.persist(t);
        }

    }

    private long convertTimestampToEpoch(String timestamp) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = format.parse(timestamp);
            long epoch = date.getTime();
            return epoch;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
