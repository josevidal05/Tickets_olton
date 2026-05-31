package com.example.tickets_android;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private List<Ticket> tickets;

    public interface OnItemClickListener {
        void onItemClick(Ticket ticket);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public TicketAdapter(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public void updateTickets(List<Ticket> newTickets) {
        this.tickets = newTickets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.id.setText(String.valueOf(ticket.getId()));
        holder.empresa.setText(ticket.getEmpresa());
        holder.contacto.setText(ticket.getContacto());
        holder.fecha_creacion.setText(ticket.getFecha());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(ticket);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tickets != null ? tickets.size() : 0;
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView id, empresa, contacto, fecha_creacion;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.item_id);
            empresa = itemView.findViewById(R.id.item_empresa);
            contacto = itemView.findViewById(R.id.item_contacto);
            fecha_creacion = itemView.findViewById(R.id.item_fecha);
        }
    }
}
