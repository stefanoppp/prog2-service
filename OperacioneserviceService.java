package com.mycompany.myapp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Service
@Transactional
public class OperacioneserviceService {

    private final Logger log = LoggerFactory.getLogger(OperacioneserviceService.class);

    public static String get_orders() {
        String url = "http://192.168.194.254:8000/api/ordenes/ordenes/";
        String jwt_token="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzdGVmYW5vMiIsImF1dGgiOiJST0xFX1VTRVIiLCJleHAiOjE3MzEwNjQyNDh9.Atbq7DfHM9u-AhfNUm0aM02EsD1GSxBhne_9Tuw3dWe-sgbNKoHAL8OMnUqtPjkJCu2QWQJtii2d71eo4PTbvQ";
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

    // validamos hora
    public boolean validate_time(LocalTime hora_orden) {
        return hora_orden.isAfter(LocalTime.parse("07:59:59")) && hora_orden.isBefore(LocalTime.parse("18:00:01"));
    }
    // validamos cliente y orden
    public boolean validate_client_action(int cliente, int accion) {
        return cliente > 0 && accion>1;
    }
    // obtenemos total 
    public boolean discount_actions(int cliente_id, int accion_id, int cantidad_solicitada){
        String base_url = "http://192.168.194.254:8000/api/reporte-operaciones/consulta_cliente_accion?";
        String clienteIdParam = "clienteId=" + cliente_id;
        String accionIdParam = "accionId=" + accion_id;
        String full_url = base_url + clienteIdParam + "&" + accionIdParam;
        
        String jwt_token="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzdGVmYW5vMiIsImF1dGgiOiJST0xFX1VTRVIiLCJleHAiOjE3MzEwNjQyNDh9.Atbq7DfHM9u-AhfNUm0aM02EsD1GSxBhne_9Tuw3dWe-sgbNKoHAL8OMnUqtPjkJCu2QWQJtii2d71eo4PTbvQ";
        try {
                URL apiUrl = new URL(full_url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            
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
                    
                    // Pasamos a json la rta de accion / cliente ID
                    ObjectMapper objectMapper = new ObjectMapper();
                    String acciones_disponibles=response.toString();
                    JsonNode rootNode = objectMapper.readTree(acciones_disponibles);
                    JsonNode consultaNode = rootNode.get("consulta");
                    // Obtenemos los datos del json para comparar si puede o no, tener la cant de acciones q quiere
                    int cantidad_real=consultaNode.get("cantidad").asInt();
                    if(cantidad_real<cantidad_solicitada){
                        return false;
                    }
                    else{
                        return true;
                    }

        } catch (Exception e) {
            return false;
        }
    }

    // encolamos ordenes
    public String encolar(String modo, int cantidad){
        return "a";
    }



    public static void main(String[] args){
        String orders = get_orders();
        ObjectMapper objectMapper = new ObjectMapper();

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
                
                
                
                // convertimos fecha ed operacion a localtime
                LocalTime horaOrden = LocalTime.parse(fechaOperacion.substring(11, 19));

                // valiadamos todo
                OperacioneserviceService servicio = new OperacioneserviceService();
                boolean esValidoTiempo = servicio.validate_time(horaOrden);
                boolean esValidoClienteAccion = servicio.validate_client_action(clienteId, accionId);
                
                // comprobamos ambas condiciones
                if (esValidoTiempo && esValidoClienteAccion) {
                    cumplenCondiciones.append(ordenNode.toString()).append("\n");
                    // verificamos si posee la cantidad solicitada
                    if(servicio.discount_actions(clienteId,accionId,cantidad)){
                        // encolamos el proceso
                        servicio.encolar(modo,cantidad);
                    }
                    else{
                        System.out.println("No posee suficientes acciones");
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
            System.out.println(noCumplenCondiciones);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}