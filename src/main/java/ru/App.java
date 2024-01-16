package ru;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class App
{
    public static ObjectMapper objectMapper = new ObjectMapper();

    public static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("[HH:mm][H:mm]");

    public static void main( String[] args) {
        try {
            File file = new File(args[0]);
            List<JsonNode> tickets = upload(file);
            tickets = filterByCities("Владивосток", "Тель-Авив", tickets);
            Set<String> carriers = tickets.stream().map(ticket -> ticket.get("carrier").asText()).collect(Collectors.toSet());
            System.out.println("Минимальное время полета между Владивостоком и Тель-Авивом:");
            for (String carrier : carriers) {
                List<JsonNode> filteredByCarrier = filterByCarrier(carrier, tickets);
                System.out.println(carrier + " - " + minFlightTime(filteredByCarrier));
            }
            double diff = Math.abs(averagePrice(tickets) - medianPrice(tickets));
            System.out.println("Разница между средней ценой и медианой для полета между Владивостоком и Тель-Авивом - " + diff);
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла");
        }
    }

    public static List<JsonNode> upload(File file) throws IOException {
        JsonNode root = objectMapper.readTree(file);
        JsonNode jsonTickets = root.get("tickets");
        List<JsonNode> jsonTicketList = new ArrayList<>();
        for (JsonNode jsonTicket : jsonTickets) {
            jsonTicketList.add(jsonTicket);
        }
        return jsonTicketList;
    }

    public static List<JsonNode> filterByCities(String originName, String destinationName, List<JsonNode> tickets) {
        return tickets.stream().filter(ticket -> ticket.get("origin_name").asText().equals(originName)
                && ticket.get("destination_name").asText().equals(destinationName)).toList();
    }

     public static List<JsonNode> filterByCarrier(String carrier, List<JsonNode> tickets) {
         return tickets.stream().filter(ticket -> ticket.get("carrier").asText().equals(carrier)).toList();
    }

    public static String minFlightTime(List<JsonNode> tickets) {
        List<Long> durations = new ArrayList<>();
        tickets.forEach(ticket -> {
            String departureTimeString = ticket.get("departure_time").asText();
            String arrivalTimeString = ticket.get("arrival_time").asText();
            LocalTime departureTime = LocalTime.parse(departureTimeString, timeFormatter);
            LocalTime arrivalTime = LocalTime.parse(arrivalTimeString, timeFormatter);
            long duration = Duration.between(departureTime, arrivalTime).abs().toSeconds();
            durations.add(duration);
        });
        long minDuration = durations.stream().mapToLong(Long::longValue).min().orElse(0);
        return LocalTime.ofSecondOfDay(minDuration).toString();
    }

    public static double averagePrice(List<JsonNode> tickets) {
        return tickets.stream().mapToDouble(ticket -> ticket.get("price").asDouble()).average().orElse(0.0);
    }

    public static double medianPrice(List<JsonNode> tickets) {
        if (tickets.isEmpty()) {
            return 0.0;
        }
        double[] prices = tickets.stream().mapToDouble(ticket -> ticket.get("price").asDouble()).toArray();
        Arrays.sort(prices);
        if (prices.length % 2 == 0) {
            double middleElem1 = prices[prices.length / 2];
            double middleElem2 = prices[prices.length / 2 - 1];
            return (middleElem1 + middleElem2) / 2;
        } else {
            return prices[prices.length / 2];
        }
    }
}
