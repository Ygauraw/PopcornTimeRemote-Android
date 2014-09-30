package eu.se_bastiaan.popcorntimeremote.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.mobsandgeeks.saripaar.Rule;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.IpAddress;
import com.mobsandgeeks.saripaar.annotation.NumberRule;
import com.mobsandgeeks.saripaar.annotation.Required;

import butterknife.ButterKnife;
import butterknife.InjectView;
import eu.se_bastiaan.popcorntimeremote.R;
import eu.se_bastiaan.popcorntimeremote.database.InstanceProvider;
import eu.se_bastiaan.popcorntimeremote.models.Instance;

public class InstanceEditorDialogFragment extends DialogFragment {

    private Boolean mIsNewInstance = false;
    private Validator mValidator;
    private String mId;

    @InjectView(R.id.nameInput)
    @Required(order = 0)
    EditText nameInput;
    @InjectView(R.id.ipInput)
    @Required(order = 1)
    @IpAddress(order = 2)
    EditText ipInput;
    @InjectView(R.id.portInput)
    @Required(order = 3)
    @NumberRule(order = 4, type = NumberRule.NumberType.INTEGER)
    EditText portInput;
    @InjectView(R.id.usernameInput)
    @Required(order = 5)
    EditText usernameInput;
    @InjectView(R.id.passwordInput)
    @Required(order = 6)
    EditText passwordInput;
    @InjectView(R.id.manualButton)
    Button manualButton;
    @InjectView(R.id.zeroconfButton)
    Button zeroconfButton;
    @InjectView(R.id.buttonLayout)
    LinearLayout buttonLayout;
    @InjectView(R.id.inputLayout)
    LinearLayout inputLayout;

    private Validator.ValidationListener mValidationListener = new Validator.ValidationListener() {
        @Override
        public void onValidationSucceeded() {
            ContentValues values = new ContentValues();
            values.put(Instance.COLUMN_NAME_NAME, nameInput.getText().toString());
            values.put(Instance.COLUMN_NAME_IP, ipInput.getText().toString());
            values.put(Instance.COLUMN_NAME_PORT, portInput.getText().toString());
            values.put(Instance.COLUMN_NAME_USERNAME, usernameInput.getText().toString());
            values.put(Instance.COLUMN_NAME_PASSWORD, passwordInput.getText().toString());


            Cursor cursor = getActivity().getContentResolver().query(Uri.withAppendedPath(InstanceProvider.IP_URI, "/" + ipInput.getText().toString()), null, null, null, null);
            Integer count = cursor.getCount();
            cursor.close();

            if(count > 0) {
                ipInput.setError(getString(R.string.ip_already_exists));
                return;
            }

            if(mIsNewInstance) {
                getActivity().getContentResolver().insert(InstanceProvider.INSTANCES_URI, values);
            } else {
                getActivity().getContentResolver().update(Uri.withAppendedPath(InstanceProvider.INSTANCES_URI, "/" + mId), values, null, null);
            }

            dismiss();
        }

        @Override
        public void onValidationFailed(View failedView, Rule<?> failedRule) {
            String message = failedRule.getFailureMessage();

            if (failedView instanceof EditText) {
                failedView.requestFocus();
                ((EditText) failedView).setError(message);
            } else {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if(args == null || !args.containsKey("_id")) {
            mIsNewInstance = true;
        } else {
            mId = args.getString("_id");
        }

        if(args != null && args.containsKey(Instance.COLUMN_NAME_IP))

        mValidator = new Validator(this);
        mValidator.setValidationListener(mValidationListener);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_instanceeditor, null, false);
        ButterKnife.inject(this, view);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setView(view)
            .setPositiveButton(R.string.save,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) { }
                    }
            )
            .setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }
            );

        if(mIsNewInstance) {
            builder.setTitle(R.string.add_instance);
        } else {
            builder.setTitle(R.string.edit_instance);
        }

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mValidator.validateAsync();
                    }
                });

                if(!mIsNewInstance) {
                    Cursor cursor = getActivity().getContentResolver().query(Uri.withAppendedPath(InstanceProvider.INSTANCES_URI, "/" + mId), null, null, null, null);
                    cursor.moveToFirst();
                    ipInput.setText(cursor.getString(1));
                    portInput.setText(cursor.getString(2));
                    nameInput.setText(cursor.getString(3));
                    usernameInput.setText(cursor.getString(4));
                    passwordInput.setText(cursor.getString(5));
                    cursor.close();

                    ipInput.setVisibility(View.VISIBLE);
                    portInput.setVisibility(View.VISIBLE);
                    usernameInput.setVisibility(View.VISIBLE);
                    passwordInput.setVisibility(View.VISIBLE);
                    buttonLayout.setVisibility(View.GONE);
                }
            }
        });

        manualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipInput.setVisibility(View.VISIBLE);
                portInput.setVisibility(View.VISIBLE);
                usernameInput.setVisibility(View.VISIBLE);
                passwordInput.setVisibility(View.VISIBLE);
                buttonLayout.setVisibility(View.GONE);
            }
        });

        zeroconfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Open ZeroConf dialog and fill selection in
            }
        });

        return dialog;
    }
}
