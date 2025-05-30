package com.example.connard;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class AllowLimitConfigRecycler extends RecyclerView.Adapter<AllowLimitConfigRecycler.ViewHolder> {
    List<GameAllowLimitInfo> gameList;

    public AllowLimitConfigRecycler(List<GameAllowLimitInfo> gameList) {
        this.gameList = gameList;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtGameName, statusGame,playedGame,limitGame;

        public ViewHolder(View itemView) {
            super(itemView);
            txtGameName = itemView.findViewById(R.id.txtGameAllowLimit);
            statusGame = itemView.findViewById(R.id.statusGameAllowLimit);
            playedGame = itemView.findViewById(R.id.playedGameAllowLimit);
            limitGame = itemView.findViewById(R.id.limitGameAllowLimit);
        }
    }
    @NonNull
    @Override
    public AllowLimitConfigRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.allow_limit_item_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AllowLimitConfigRecycler.ViewHolder holder, int position) {
        GameAllowLimitInfo game = gameList.get(position);
        holder.txtGameName.setText(game.getName());
        holder.statusGame.setText("Trạng thái: " + game.getStatus());
        holder.playedGame.setText("Đã chơi: " + game.getPlayed());
        holder.limitGame.setText("Giới hạn thời gian chơi mỗi ngày: " + game.getLimit());
    }
    public void updateGameList(List<GameAllowLimitInfo> newGameList) {
        this.gameList = newGameList;
        notifyDataSetChanged(); // Thông báo cho adapter rằng dữ liệu đã thay đổi
    }
    @Override
    public int getItemCount() {
        return gameList.size();
    }
}