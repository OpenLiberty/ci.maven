package application.transaction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // Epoch time
    @Column(name = "cur_time")
    private long time;
    
    public void setTime(long time) {
    	this.time = time;
    }
    
    public long getTime() {
    	return time;
    }
    
    // Price
    private float price;
    
    public void setPrice(float price) {
    	this.price = price;
    }
    
    public float getPrice() {
    	return price;
    }
    
    // Buy Amount
    @Column(name = "buy_amount")
    private float buy;
    
    public void setBuy(float buy) {
    	this.buy = buy;
    }
    
    public float getBuy() {
    	return buy;
    }
    
    // Sell Amount
    @Column(name = "sell_amount")
    private float sell;
    
    public void setSell(float sell) {
    	this.sell = sell;
    }
    
    public float getSell() {
    	return sell;
    }
    
}
