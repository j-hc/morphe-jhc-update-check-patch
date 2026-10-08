package app.morphe.extension.jhc;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class CustomDialog {
    private CustomDialog() {
    }

    public static Pair<Dialog, LinearLayout> create(
            Context context,
            CharSequence title,
            CharSequence message,
            EditText editText,
            CharSequence okButtonText,
            Runnable onOkClick,
            Runnable onCancelClick,
            CharSequence neutralButtonText,
            Runnable onNeutralClick,
            boolean dismissDialogOnNeutralClick
    ) {
        return create(
                context,
                title,
                message,
                editText,
                okButtonText,
                onOkClick,
                onCancelClick,
                neutralButtonText,
                onNeutralClick,
                dismissDialogOnNeutralClick,
                null,
                null,
                true,
                true
        );
    }

    public static Pair<Dialog, LinearLayout> create(
            Context context,
            CharSequence title,
            CharSequence message,
            EditText editText,
            CharSequence okButtonText,
            Runnable onOkClick,
            Runnable onCancelClick,
            CharSequence neutralButtonText,
            Runnable onNeutralClick,
            boolean dismissDialogOnNeutralClick,
            boolean accentOkButton
    ) {
        return create(
                context,
                title,
                message,
                editText,
                okButtonText,
                onOkClick,
                onCancelClick,
                neutralButtonText,
                onNeutralClick,
                dismissDialogOnNeutralClick,
                null,
                null,
                true,
                accentOkButton
        );
    }

    public static Pair<Dialog, LinearLayout> create(
            Context context,
            CharSequence title,
            CharSequence message,
            EditText editText,
            CharSequence okButtonText,
            Runnable onOkClick,
            Runnable onCancelClick,
            CharSequence neutralButtonText,
            Runnable onNeutralClick,
            boolean dismissDialogOnNeutralClick,
            CharSequence additionalButtonText,
            Runnable onAdditionalClick,
            boolean dismissDialogOnAdditionalClick,
            boolean accentOkButton
    ) {
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int foregroundColor = isDarkMode ? Color.WHITE : Color.BLACK;
        int backgroundColor = isDarkMode ? Color.rgb(48, 48, 48) : Color.WHITE;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 24), dp(context, 16), dp(context, 24), dp(context, 24));
        content.setBackground(background(backgroundColor, dp(context, 28)));

        if (!TextUtils.isEmpty(title)) {
            TextView titleView = new TextView(context);
            titleView.setText(title);
            titleView.setTextSize(18);
            titleView.setTextColor(foregroundColor);
            titleView.setGravity(Gravity.CENTER);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            content.addView(titleView, marginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0,
                    0,
                    0,
                    dp(context, 16)
            ));
        }

        if (editText != null || message != null) {
            ScrollView scrollView = new ScrollView(context);
            scrollView.setVerticalScrollBarEnabled(false);

            if (editText != null) {
                ViewGroup parent = (ViewGroup) editText.getParent();
                if (parent != null) {
                    parent.removeView(editText);
                }
                editText.setTextColor(foregroundColor);
                scrollView.addView(editText);
            } else {
                TextView messageView = new TextView(context);
                messageView.setText(message);
                messageView.setTextSize(16);
                messageView.setTextColor(foregroundColor);
                scrollView.addView(messageView);
            }

            content.addView(scrollView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }

        LinearLayout buttons = new LinearLayout(context);
        buttons.setGravity(Gravity.END);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setClipChildren(false);
        content.addView(buttons, marginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0,
                dp(context, 16),
                0,
                0
        ));

        if (neutralButtonText != null && onNeutralClick != null) {
            buttons.addView(createButton(
                    context,
                    dialog,
                    neutralButtonText,
                    onNeutralClick,
                    false,
                    dismissDialogOnNeutralClick,
                    foregroundColor,
                    backgroundColor
            ));
        }
        if (additionalButtonText != null && onAdditionalClick != null) {
            LinearLayout additionalButtons = new LinearLayout(context);
            additionalButtons.setGravity(Gravity.END);
            additionalButtons.setOrientation(LinearLayout.HORIZONTAL);
            additionalButtons.setClipChildren(false);
            content.addView(additionalButtons, marginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0,
                    dp(context, 4),
                    0,
                    dp(context, 4)
            ));
            additionalButtons.addView(createButton(
                    context,
                    dialog,
                    additionalButtonText,
                    onAdditionalClick,
                    false,
                    dismissDialogOnAdditionalClick,
                    foregroundColor,
                    backgroundColor
            ));
        }
        if (onCancelClick != null) {
            buttons.addView(createButton(
                    context,
                    dialog,
                    "Cancel",
                    onCancelClick,
                    false,
                    true,
                    foregroundColor,
                    backgroundColor
            ));
        }
        if (onOkClick != null) {
            buttons.addView(createButton(
                    context,
                    dialog,
                    okButtonText == null ? "OK" : okButtonText,
                    onOkClick,
                    accentOkButton,
                    true,
                    foregroundColor,
                    backgroundColor
            ));
        }

        dialog.setContentView(content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        return new Pair<>(dialog, content);
    }

    private static Button createButton(
            Context context,
            Dialog dialog,
            CharSequence text,
            Runnable action,
            boolean accent,
            boolean dismiss,
            int foregroundColor,
            int backgroundColor
    ) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(foregroundColor);
        button.setMinHeight(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        button.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        button.setBackground(null);
        button.setOnClickListener(view -> {
            action.run();
            if (dismiss) {
                dialog.dismiss();
            }
        });
        return button;
    }

    private static GradientDrawable background(int color, int cornerRadius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(cornerRadius);
        return drawable;
    }

    private static LinearLayout.LayoutParams marginLayoutParams(
            int width,
            int height,
            int left,
            int top,
            int right,
            int bottom
    ) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
