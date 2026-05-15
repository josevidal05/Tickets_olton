package com.example.tickets_android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TicketList {
    private List<Ticket> ticketList;

    public List<Ticket> getTicketList() {
        return ticketList;
    }


    public TicketList(JSONArray array) {
        ticketList = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject jsonElement = array.getJSONObject(i);
                Ticket playlist = new Ticket(jsonElement);
                ticketList.add(playlist);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
