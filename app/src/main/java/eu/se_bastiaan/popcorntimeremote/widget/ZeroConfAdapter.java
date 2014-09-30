package eu.se_bastiaan.popcorntimeremote.widget;

import android.content.Context;
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
import eu.se_bastiaan.popcorntimeremote.Constants;
import eu.se_bastiaan.popcorntimeremote.R;
import eu.se_bastiaan.popcorntimeremote.models.Instance;
import eu.se_bastiaan.popcorntimeremote.network.ZeroConfClient;

public class ZeroConfAdapter extends BaseAdapter {

    private static class ViewHolder {
        public ViewHolder(View v) {
            ButterKnife.inject(this, v);
        }
        @InjectView(R.id.text1)
        public TextView name;
        @InjectView(R.id.text2)
        public TextView ip;
    }

    private List<Instance> mObjects;
    private LayoutInflater mInflater;
    private ZeroConfClient mZeroConf;

    private JsonParser mParser = new JsonParser();
    private Gson mGson = new Gson();

    public ZeroConfAdapter(Context context) {
        super();
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mObjects = new ArrayList<Instance>();
        mZeroConf = new ZeroConfClient(context, Constants.ZEROCONF_PORT);
        mZeroConf.startClient(new Runnable() {
            @Override
            public void run() {
                JsonElement message = mParser.parse(mZeroConf.getIncoming().getMessage());
                if(message.isJsonObject()) {
                    JsonObject payload = (JsonObject) message;
                    if(payload.has("action") && payload.get("action").getAsString().equals("announce")) {
                        Instance model = mGson.fromJson(message, Instance.class);
                        if (!mObjects.contains(model)) {
                            mObjects.add(model);
                            notifyDataSetChanged();
                        }
                    }
                }
            }
        });
        mZeroConf.sendMessage("{\"action\":\"search\"}");
    }

    public int getCount() {
        return mObjects.size();
    }

    public Object getItem(int i) {
        return mObjects.get(i);
    }

    public long getItemId(int i) {
        return i;
    }

    public View getView(int i, View convertView, ViewGroup parent) {
        ViewHolder holder;
        Instance model = mObjects.get(i);

        if(convertView == null) {
            convertView = mInflater.inflate(R.layout.fragment_listinstance_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        holder.name.setText(model.name);
        holder.ip.setText(model.ip);

        return convertView;
    }

}
