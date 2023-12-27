package com.mycompany.myapp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.boot.configurationprocessor.json.JSONStringer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.Date;
import java.util.TimerTask;
import java.util.Calendar;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
@Service
@Transactional
public class OperacioneserviceService {

    private final Logger log = LoggerFactory.getLogger(OperacioneserviceService.class);

    public static String get(String url, String jwt_token){
        try {
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            // establecer el encabezado de autorización con el token JWT
            connection.setRequestProperty("Authorization", "Bearer " + jwt_token);
            int responseCode = connection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("La solicitud no fue exitosa. Respuesta: " + responseCode);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            connection.disconnect();
            return response.toString();

        } catch (IOException e) {
            return "Error";
        }
    }
    public static String get_orders() {
        String url = "http://192.168.194.254:8000/api/ordenes/ordenes/";
        String jwt_token="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzdGVmYW5vMiIsImF1dGgiOiJST0xFX1VTRVIiLCJleHAiOjE3MzEwNjQyNDh9.Atbq7DfHM9u-AhfNUm0aM02EsD1GSxBhne_9Tuw3dWe-sgbNKoHAL8OMnUqtPjkJCu2QWQJtii2d71eo4PTbvQ";
        return get(url, jwt_token);
    }

    // validamos hora
    public boolean validate_time(LocalTime hora_orden) {
        return hora_orden.isAfter(LocalTime.parse("07:59:59")) && hora_orden.isBefore(LocalTime.parse("18:00:01"));
    }
    // validamos cliente y orden
    public boolean validate_client_action(int cliente, int accion) {
        return cliente > 0 && accion>1;
    }
    // obtenemos total 
    public static int resta(int cantidad_solicitada, int cantidad_real){
        int resta=cantidad_solicitada-cantidad_real;
        return resta;
    }
    public static int suma(int cantidad_solicitada, int cantidad_real){
        int suma=cantidad_solicitada+cantidad_real;
        return suma;
    }
    public class MutableBoolean {
        private boolean value;
    
        public MutableBoolean(boolean value) {
            this.value = value;
        }
    
        public boolean getValue() {
            return value;
        }
    
        public void setValue(boolean value) {
            this.value = value;
        }
    }
    public static long calcularDelay(LocalTime horaActual, LocalTime horaEjecucion) {
        long delay;
        if (horaActual.isAfter(horaEjecucion)) {
            long segundosHastaMediaNoche = LocalTime.MAX.toSecondOfDay() - horaActual.toSecondOfDay();
            long segundosDesdeInicioDelDia = horaEjecucion.toSecondOfDay();
            delay = segundosHastaMediaNoche + segundosDesdeInicioDelDia;
        } 
        else {
            delay = horaEjecucion.toSecondOfDay() - horaActual.toSecondOfDay();
        }
        return delay;
    }
    public static void process(Queue<JSONObject> principioQueue, Queue<JSONObject> ahoraQueue, Queue<JSONObject> finQueue){
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        LocalTime horaActual = LocalTime.now();

        long delayHasta9 = calcularDelay(horaActual, LocalTime.of(9, 0));
        long delayHasta18 = calcularDelay(horaActual, LocalTime.of(22, 32));

        // Procesamos cola inmediata
        System.out.println("COLA INMEDIATA");
        operaciones(ahoraQueue);
        scheduler.schedule(() -> {
            System.out.println("COLA 9 AM");
            operaciones(principioQueue);
        }, delayHasta9, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            System.out.println("COLA 6 PM");
            operaciones(finQueue);
        }, delayHasta18, TimeUnit.SECONDS);

    }
    public static Queue<JSONObject> operaciones(Queue<JSONObject> cola){
        String base_url = "http://192.168.194.254:8000/api/reporte-operaciones/consulta_cliente_accion?";
        String jwt_token="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzdGVmYW5vMiIsImF1dGgiOiJST0xFX1VTRVIiLCJleHAiOjE3MzEwNjQyNDh9.Atbq7DfHM9u-AhfNUm0aM02EsD1GSxBhne_9Tuw3dWe-sgbNKoHAL8OMnUqtPjkJCu2QWQJtii2d71eo4PTbvQ";
        Queue<JSONObject> colaModificada=new LinkedList<>();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
        @Override
        public void run() {

            while (!cola.isEmpty()) {
                // Orden que estamos procesando
                JSONObject elemento = cola.poll();
                // Empieza el codigo
                try {
                    String tipoOperacion = elemento.getString("operacion");
                    int cliente_id = elemento.getInt("cliente");
                    int accion_id = elemento.getInt("accionId");
                    int cantidad_solicitada = elemento.getInt("cantidad");
            
                    String clienteIdParam = "clienteId=" + cliente_id;
                    String accionIdParam = "accionId=" + accion_id;
                    String full_url = base_url + clienteIdParam + "&" + accionIdParam;

                    JSONObject accionesCliente = new JSONObject(get(full_url, jwt_token));
            
                    Object cantidadActual = accionesCliente.get("cantidadActual");

                    if ("COMPRA".equals(tipoOperacion)) {
                        System.out.println("Orden de COMPRA");
                        if (cantidadActual == JSONObject.NULL) {
                            System.out.println("No posee acciones");
                        } 
                        else{
                            System.out.println("Usted solicito " +cantidadActual+ " acciones");
                            System.out.println("Se agregan " +cantidad_solicitada+ " acciones");
                        }
                        cantidadActual=+cantidad_solicitada;
                        accionesCliente.put("cantidadActual", cantidadActual);
                        System.out.println("Ahora posee " + cantidadActual);
                        elemento.put("operacionExitosa", true);
                        elemento.put("operacionesObservaciones", "ok");
                    }
                    
                    if ("VENTA".equals(tipoOperacion)) {
                        System.out.println("Orden de VENTA");

                        if (cantidadActual == JSONObject.NULL ) {
                            System.out.println("No posee suficientes acciones");
                            elemento.put("operacionExitosa", false);
                            elemento.put("operacionesObservaciones", "No posee suficientes acciones");
                        } 
                        else {
                            System.out.println("Posee " + cantidadActual + " acciones");
                            System.out.println("Se restan " + cantidad_solicitada + " acciones");
                            cantidadActual=-cantidad_solicitada;
                            System.out.println("Posee " + cantidadActual + " acciones");
                            elemento.put("operacionExitosa", true);
                            elemento.put("operacionesObservaciones", "ok");
                        }
                    }
                    colaModificada.offer(elemento);
                    System.out.println(elemento.toString()+ "\n");

                } 
                catch(Exception e){
                    System.out.println(e);
                }
            }
            timer.cancel();
            }
        }, 100);
        return colaModificada;
    }

    public static void main(String[] args){
        String orders = get_orders();
        ObjectMapper objectMapper = new ObjectMapper();

        // Colas
        Queue<JSONObject> principioQueue = new LinkedList<>();
        Queue<JSONObject> ahoraQueue = new LinkedList<>();
        Queue<JSONObject> finQueue = new LinkedList<>();

        try {
            // deserializar el JSON a un objeto Java
            JsonNode rootNode = objectMapper.readTree(orders);
            JsonNode ordenesNode = rootNode.get("ordenes");

            // listas que almacenan condiciones que si y que no
            StringBuilder cumplenCondiciones = new StringBuilder();
            StringBuilder noCumplenCondiciones = new StringBuilder();

            for (JsonNode ordenNode : ordenesNode) {
                String fechaOperacion = ordenNode.get("fechaOperacion").asText();
                int clienteId = ordenNode.get("cliente").asInt();
                int accionId = ordenNode.get("accionId").asInt();
                int cantidad=ordenNode.get("cantidad").asInt();
                String modo=ordenNode.get("modo").asText();
                String tipoOperacion=ordenNode.get("operacion").asText();

                // convertimos fecha ed operacion a localtime
                LocalTime horaOrden = LocalTime.parse(fechaOperacion.substring(11, 19));

                // valiadamos todo
                OperacioneserviceService servicio = new OperacioneserviceService();
                boolean esValidoTiempo = servicio.validate_time(horaOrden);
                boolean esValidoClienteAccion = servicio.validate_client_action(clienteId, accionId);
                
                // comprobamos las 3 condiciones
                if (esValidoTiempo && esValidoClienteAccion) {
                    cumplenCondiciones.append(ordenNode.toString()).append("\n");
                    if("PRINCIPIODIA".equals(modo)){
                        try{
                            JSONObject ordenPrincipio = new JSONObject(ordenNode.toString());
                            principioQueue.offer(ordenPrincipio);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    if("AHORA".equals(modo)){
                        try{
                            JSONObject ordenAhora = new JSONObject(ordenNode.toString());
                            ahoraQueue.offer(ordenAhora);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    if("FINDIA".equals(modo)){
                        try{
                            JSONObject ordenFin = new JSONObject(ordenNode.toString());
                            finQueue.offer(ordenFin);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                } 
                else {
                    StringBuilder razonesNoCumple = new StringBuilder();
                    if (!esValidoTiempo) {
                        razonesNoCumple.append("No cumple con el horario permitido\n");
                    }
                    if (!esValidoClienteAccion) {
                        razonesNoCumple.append("No tiene cliente válido o acción de compañía\n");
                    }
                    noCumplenCondiciones.append("Orden: ").append(ordenNode.toString()).append("\n")
                            .append("Razones: ").append(razonesNoCumple).append("\n");
                }
            }
            System.out.println("Ordenes encoladas exitosamente");
            process(principioQueue, ahoraQueue, finQueue);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
