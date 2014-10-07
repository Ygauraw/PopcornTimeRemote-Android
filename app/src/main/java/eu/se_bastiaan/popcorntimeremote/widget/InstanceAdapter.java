package eu.se_bastiaan.popcorntimeremote.widget;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import eu.se_bastiaan.popcorntimeremote.Constants;
import eu.se_bastiaan.popcorntimeremote.R;
import eu.se_bastiaan.popcorntimeremote.models.Instance;
import eu.se_bastiaan.popcorntimeremote.network.ZeroConfClient;
import eu.se_bastiaan.popcorntimeremote.utils.LogUtils;

/**
 * Created by Sebastiaan on 02-10-14.
 */
public class InstanceAdapter extends BaseAdapter {

    class ViewHolder {
        public ViewHolder(View v) {
            ButterKnife.inject(this, v);
        }
        @InjectView(R.id.text1)
        public TextView name;
        @Optional
        @InjectView(R.id.text2)
        public TextView ip;
    }

    enum ViewType { HEADER, SAVED_INSTANCE, ZEROCONF_INSTANCE };

    private ZeroConfClient mZeroConf;
    private JsonParser mParser = new JsonParser();
    private Gson mGson = new Gson();
    private LayoutInflater mInflater;
    private Context mContext;

    private ArrayList<Instance> mZeroConfInstances = new ArrayList<Instance>();
    private ArrayList<Instance> mHiddenSavedInstances = new ArrayList<Instance>();
    private ArrayList<Instance> mSavedInstances = new ArrayList<Instance>();

    public InstanceAdapter(Context context, Cursor savedInstances) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mZeroConf = new ZeroConfClient(context, Constants.ZEROCONF_PORT);
        mZeroConf.setIncomingMessageAnalyseRunnable(new Runnable() {
            @Override
            public void run() {
                LogUtils.d("InstanceAdapter", "Parsing incoming message");
                JsonElement message = mParser.parse(mZeroConf.getIncoming().getMessage());
                if (message.isJsonObject()) {
                    JsonObject payload = (JsonObject) message;
                    if (payload.has("action") && payload.get("action").getAsString().equals("announce")) {
                        Instance model = mGson.fromJson(message, Instance.class);
                        if (!mZeroConfInstances.contains(model)) {
                            LogUtils.d("InstanceAdapter", "Adding zeroconf instance to list");
                            mZeroConfInstances.add(model);
                            recheckCursorData();
                            notifyDataSetChanged();
                        }
                    }
                }
            }
        });
        if (!mZeroConf.isStarted()) mZeroConf.startClient();
        mZeroConf.sendMessage("{\"action\":\"search\"}");

        swapCursor(savedInstances);
    }

    public void swapCursor(Cursor cursor) {
        LogUtils.d("InstanceAdapter", "swapCursor");

        mSavedInstances.clear();
        mHiddenSavedInstances.clear();

        if(cursor == null || cursor.getCount() <= 0) {
            notifyDataSetChanged();
            return;
        }

        cursor.moveToFirst();
        do {
            Instance instance = new Instance();
            instance.id = cursor.getString(0);
            instance.ip = cursor.getString(1);
            instance.port = cursor.getString(2);
            instance.name = cursor.getString(3);
            instance.username = cursor.getString(4);
            instance.password = cursor.getString(5);
            if(!mZeroConfInstances.contains(instance)) {
                mSavedInstances.add(instance);
            } else {
                mHiddenSavedInstances.add(instance);
            }
        } while(cursor.moveToNext());

        notifyDataSetChanged();
    }

    public void recheckCursorData() {
        for(int i = 0; i < mHiddenSavedInstances.size(); i++) {
            Instance instance = mHiddenSavedInstances.get(i);
            if(mZeroConfInstances.contains(instance)) {
                mHiddenSavedInstances.remove(instance);
                mSavedInstances.add(instance);
            }
        }

        for(int i = 0; i < mZeroConfInstances.size(); i++) {
            Instance instance = mZeroConfInstances.get(i);
            Integer index = mSavedInstances.indexOf(instance);
            if(index > -1) {
                mZeroConfInstances.remove(instance);
                instance = mSavedInstances.get(index);
                mSavedInstances.remove(instance);
                mZeroConfInstances.add(instance);
            }
        }
    }

    @Override
    public int getCount() {
        Integer count = 0;
        if(mSavedInstances.size() > 0) {
            count += mSavedInstances.size() + 1;
        }
        if(mZeroConfInstances.size() > 0) {
            count += mZeroConfInstances.size() + 1;
        }
        return count;
    }

    @Override
    public Instance getItem(int position) {
        Integer type = getItemViewType(position);
        int topSize = mZeroConfInstances.size() > 0 ? mZeroConfInstances.size() + 2 : 1;

        switch (type) {
            case 0: // ViewType.HEADER.ordinal()
                Instance instance = new Instance();
                instance.name = position == 0 && topSize > 1 ? mContext.getString(R.string.available_instances) : mContext.getString(R.string.saved_instances);
                return instance;

            case 1: // ViewType.SAVED_INSTANCE.ordinal()
                return mSavedInstances.get(position - topSize);

            case 2: // ViewType.ZEROCONF_INSTANCE.ordinal()
                return mZeroConfInstances.get(position - 1);
        }

        return null;
    }

    @Override
    public int getItemViewType(int position) {
        int savedInstanceSize = mSavedInstances.size();
        int zeroConfInstanceSize = mZeroConfInstances.size();

        if(position == 0) {
            return ViewType.HEADER.ordinal();
        } else if(zeroConfInstanceSize > 0 && position <= zeroConfInstanceSize) {
            return ViewType.ZEROCONF_INSTANCE.ordinal();
        } else if(savedInstanceSize > 0 && zeroConfInstanceSize > 0 && position == zeroConfInstanceSize + 1) {
            return ViewType.HEADER.ordinal();
        } else {
            return ViewType.SAVED_INSTANCE.ordinal();
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.fragment_listinstance_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        Instance instance = getItem(position);
        ViewType viewType = ViewType.values()[getItemViewType(position)];

        if(viewType == ViewType.HEADER) {
            holder.name.setText(instance.name);
        } else {
            holder.name.setText(instance.name);
            holder.ip.setText(instance.ip);
        }

        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != ViewType.HEADER.ordinal();
    }
}
