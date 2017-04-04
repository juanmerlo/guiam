package com.jmk.guiam;

/**
 * Created by juanmartin on 3/11/2016.
 */

public class ArrayAccuracy {

    private float[] array = new float[20];
    private int contador = 0;
    private String name;

    public ArrayAccuracy(String name){

        this.name = name;
        inicializarArray();
    }

    public void inicializarArray(){

        for(int i = 0;i< array.length;i++){

            //Inicializo el array
            array[i] = 10000;
        }
    }

    public float getPromedio(){

        float suma = 0;

        for(int i = 0;i<array.length;i++){

            suma += array[i];
        }

        return (suma / array.length);
    }

    public void insertarAccuracy(float accuracy){

        if(contador == array.length) contador = 0;

        array[contador] = accuracy;

        contador++;
    }

    public String getName() {

        return this.name;
    }

    static ArrayAccuracy getMejor(ArrayAccuracy a, ArrayAccuracy b, ArrayAccuracy c){

        ArrayAccuracy mejor = new ArrayAccuracy("");

        if(a.getPromedio()<= b.getPromedio() && a.getPromedio()<=c.getPromedio() )
            mejor = a;
        if(b.getPromedio()<= a.getPromedio() && b.getPromedio()<=c.getPromedio() )
            mejor =  b;
        if(c.getPromedio()<= a.getPromedio() && c.getPromedio()<=b.getPromedio() )
            mejor =  c;

        return mejor;
    }

}
