package com.yannik.anki;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yannik.sharedvalues.CommonIdentifiers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.yannik.sharedvalues.CommonIdentifiers.P2W_COLLECTION_LIST_DECK_COUNT;
import static com.yannik.sharedvalues.CommonIdentifiers.P2W_COLLECTION_LIST_DECK_ID;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class CollectionFragment extends Fragment implements AbsListView.OnItemClickListener,
        WearMainActivity.JsonReceiver {

    private static final String TAG = "CollectionFragment";

    ArrayList<Deck> mDecks = new ArrayList<>();
    View collectionListContainer;
    private OnFragmentInteractionListener mListener;
    private Preferences settings;

    private AbsListView mListView;

    private BaseAdapter mAdapter;
    private TextView mNoDecksMessageTextView;

    public CollectionFragment() {
    }

    public void setSettings(Preferences settings) {
        this.settings = settings;
        applySettings();
    }

    public void applySettings() {
        if (settings == null) return;
        collectionListContainer.setBackgroundResource(R.drawable.round_rect_day);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new DayNightArrayAdapter(getActivity(), settings, mDecks);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collection, container, false);
        collectionListContainer = view.findViewById(R.id.collectionListContainer);
        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        mNoDecksMessageTextView = (TextView) view.findViewById(android.R.id.empty);

        applySettings();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        applySettings();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setChooseDeckListener(OnFragmentInteractionListener listener) {
        this.mListener = listener;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            mListener.onFragmentInteraction(mDecks.get(position).deckID);
        }
    }

    @Override
    public void onJsonReceive(String path, JSONObject js) {
        if (path.equals(CommonIdentifiers.P2W_COLLECTION_LIST)) {
            JSONArray collectionNames = js.names();
            if (collectionNames == null) return;

            List<Deck> decks = new ArrayList<>();

            for (int i = 0; i < collectionNames.length(); i++) {
                try {
                    String colName = collectionNames.getString(i);
                    JSONObject deckObject = js.getJSONObject(colName);
                    long deckID = deckObject.getLong(P2W_COLLECTION_LIST_DECK_ID);
                    final JSONArray deckCounts = new JSONArray(deckObject.getString(P2W_COLLECTION_LIST_DECK_COUNT));

                    Deck newDeck = new Deck(colName, deckID, deckCounts.getInt(2), deckCounts.getInt(0), deckCounts.getInt(1));
                    decks.add(newDeck);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            mDecks.clear();

            for (Deck d : decks) {
                final int[] studyCounts = d.getTotalCardsToStudy(decks);
                if (studyCounts[0] + studyCounts[1] + studyCounts[2] > 0)
                    mDecks.add(d);
            }

            Collections.sort(mDecks, new Comparator<Deck>() {
                @Override
                public int compare(Deck d1, Deck d2) {
                    return new ListComparator<String>().compare(Arrays.asList(d1.nameTokens), Arrays.asList(d2.nameTokens));
                }
            });

            if (mListView == null) {
                return;
            }

            if (mDecks.isEmpty()) {
                mNoDecksMessageTextView.setVisibility(View.VISIBLE);
                mListView.setVisibility(View.GONE);
            } else {
                mNoDecksMessageTextView.setVisibility(View.GONE);
                mListView.setVisibility(View.VISIBLE);
            }
            mAdapter.notifyDataSetChanged();

        } else {
            Log.w(TAG, "Received message with un-managed path");
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(long id);
    }

    /**
     * Customised adapter for displaying list of deck names.
     * Supports day and night mode.
     */
    private static class DayNightArrayAdapter extends BaseAdapter {
        private final Context mContext;
        private final List<Deck> mDecks;
        private final Preferences mSettings;

        private static class DeckViewHolder {
            RelativeLayout catLayout;
            TextView catName;
            TextView catNumber;
        }

        DayNightArrayAdapter(Context parContext, Preferences settings, List<Deck> parDecks) {
            mContext = parContext;
            mDecks = parDecks;
            mSettings = settings;
        }

        @Override
        public int getCount() {
            if (mDecks != null) {
                return mDecks.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            return mDecks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, View view, @NonNull ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.collection_list_item, parent, false);
            }

            DeckViewHolder viewHolder = (DeckViewHolder) view.getTag();
            if (viewHolder == null) {
                viewHolder = new DeckViewHolder();
                viewHolder.catLayout = (RelativeLayout) view.findViewById(R.id.colllist__mainLayout);
                viewHolder.catName = (TextView) view.findViewById(R.id.colllist__textcategory);
                viewHolder.catNumber = (TextView) view.findViewById(R.id.colllist__textNumber);
                view.setTag(viewHolder);
            }

            // setting here values to the fields of my items from my fan object
            Deck oneDeck = mDecks.get(position);

            StringBuilder deckName = new StringBuilder();
            for (int i = 0; i < oneDeck.nameTokens.length - 1; i++) {
                deckName.append("  ↳ ");
            }
            deckName.append(oneDeck.getSubdeckName());
            viewHolder.catName.setText(deckName.toString());
            // Using fromHtml to allow easy one character coloration
            viewHolder.catNumber.setText(Html.fromHtml(sumCountsForDeck(oneDeck)));

            // coloring background
            if (mSettings == null || mSettings.isDayMode()) {
                viewHolder.catName.setTextColor(mContext.getResources().getColor(R.color.dayTextColor));
                viewHolder.catNumber.setTextColor(mContext.getResources().getColor(R.color.dayTextColor));
            } else {
                viewHolder.catName.setTextColor(mContext.getResources().getColor(R.color.nightTextColor));
                viewHolder.catNumber.setTextColor(mContext.getResources().getColor(R.color.nightTextColor));
            }

            return view;
        }

        private String sumCountsForDeck(Deck targetDeck) {
            int numNewCards = 0;
            int numLearningCards = 0;
            int numReviewingCards = 0;
            for (Deck deck : mDecks) {
                if (deck.getName().equals(targetDeck.getName()) || deck.isSubdeckOf(targetDeck)) {
                    numNewCards += deck.newCount;
                    numLearningCards += deck.learningCount;
                    numReviewingCards += deck.reviewCount;
                }
            }

            // format and colorize to produce HTML
            StringBuilder res = new StringBuilder();

            if (numNewCards == 0) {
                res.append("<font color='grey'>0</font>");
            } else {
                res.append("<font color='blue'>");
                res.append(numNewCards);
                res.append("</font>");
            }
            res.append(" ");
            if (numLearningCards == 0) {
                res.append("<font color='grey'>0</font>");
            } else {
                res.append("<font color='red'>");
                res.append(numLearningCards);
                res.append("</font>");
            }
            res.append(" ");
            if (numReviewingCards == 0) {
                res.append("<font color='grey'>0</font>");
            } else {
                res.append("<font color='green'>");
                res.append(numReviewingCards);
                res.append("</font>");
            }

            return res.toString();
        }
    }

    private static class Deck {
        public final String[] nameTokens;
        public final long deckID;
        public final int newCount;
        public final int learningCount;
        public final int reviewCount;

        public Deck(String deckName, long deckID, int newCount, int learningCount, int reviewCount) {
            nameTokens = deckName.split("::");
            this.deckID = deckID;
            this.newCount = newCount;
            this.learningCount = learningCount;
            this.reviewCount = reviewCount;
        }

        String getName() {
            return TextUtils.join("::", nameTokens);
        }

        String getSubdeckName() {
            return nameTokens[nameTokens.length - 1];
        }

        boolean isSubdeckOf(Deck d) {
            return getName().contains(d.getName() + "::");
        }

        @Override
        public String toString() {
            return "Deck{" +
                    "nameTokens=" + Arrays.toString(nameTokens) +
                    ", deckID=" + deckID +
                    ", newCount=" + newCount +
                    ", learningCount=" + learningCount +
                    ", reviewCount=" + reviewCount +
                    '}';
        }

        public int[] getTotalCardsToStudy(List<Deck> allDecks) {
            int numNewCards = 0;
            int numLearningCards = 0;
            int numReviewingCards = 0;
            for (Deck deck : allDecks) {
                if (deck.getName().equals(getName()) || deck.isSubdeckOf(this)) {
                    numNewCards += deck.newCount;
                    numLearningCards += deck.learningCount;
                    numReviewingCards += deck.reviewCount;
                }
            }

            return new int[]{numNewCards, numLearningCards, numReviewingCards};
        }
    }
}
