package com.example.carlosguzman.gasolinamexico;

/**
 * Created by 501820531 on 4/17/2016.
 */
public class gasolinaClass {

    private int fruit_resource;
    private String fruit_name;
    private String fruit_qty;



    public gasolinaClass(int fruit_resource, String fruit_name, String fruit_qty) {
        super();
        this.setFruit_resource(fruit_resource);
        this.setFruit_name(fruit_name);
        this.setFruit_qty(fruit_qty);
    }
    public int getFruit_resource() {
        return fruit_resource;
    }
    public void setFruit_resource(int fruit_resource) {
        this.fruit_resource = fruit_resource;
    }
    public String getFruit_name() {
        return fruit_name;
    }
    public void setFruit_name(String fruit_name) {
        this.fruit_name = fruit_name;
    }
    public String getFruit_qty() {
        return fruit_qty;
    }
    public void setFruit_qty(String fruit_qty) {
        this.fruit_qty = fruit_qty;
    }

}

