package com.ttpCorp.carlosguzman.preciogasolinamexico;

/**
 * Created by 501820531 on 4/17/2016.
 */
public class gasolinaClass {
    private String gas_resource;
    private String gas_name;
    private String gas_price;
    private String prevgas_price;
    private String nextgas_price;

    public gasolinaClass(String gas_resource, String gas_name, String gas_price,String prevGas_price,String next_Gasprice) {
        super();
        this.setGas_name(gas_name);
        this.setGas_price(gas_price);
        this.setPrevGas_price(prevGas_price);
        this.setGas_resource(gas_resource);
        this.setNextGas_price(next_Gasprice);
    }

    public String getGas_resource() {
        return gas_resource;
    }

    public void setGas_resource(String gas_resource) {
        this.gas_resource = gas_resource;
    }

    public String getGas_name() {
        return gas_name;
    }

    public void setGas_name(String gas_name) {
        this.gas_name = gas_name;
    }

    public String getGas_price() {
        return gas_price;
    }

    public void setPrevGas_price(String gas_price) {
        this.prevgas_price = gas_price;
    }
    public String getPrevGas_price() {
        return prevgas_price;
    }

    public void setGas_price(String gas_price) {
        this.gas_price = gas_price;
    }

    public void setNextGas_price(String gas_price) {
        this.nextgas_price = gas_price;
    }
    public String getNextGas_price() {
        return nextgas_price;
    }
}

