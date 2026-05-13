package com.codram.terecojo.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.codram.terecojo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RatingDialogFragment extends DialogFragment {

    public interface OnRatingSubmitListener {
        void onSubmit(int estrellas, String comentario);
    }

    private OnRatingSubmitListener listener;

    public void setOnRatingSubmitListener(OnRatingSubmitListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rating, null);

        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        TextInputEditText etComment = view.findViewById(R.id.etComment);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmit);

        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                btnSubmit.setEnabled(rating > 0);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    int estrellas = (int) ratingBar.getRating();
                    String comentario = etComment.getText() != null ? etComment.getText().toString().trim() : null;
                    listener.onSubmit(estrellas, comentario);
                }
                dismiss();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view);
        return builder.create();
    }
}
