package com.adeneche.spotifystreamer;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.adeneche.spotifystreamer.parcels.TrackParcel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class TopTenActivityFragment extends Fragment {
    private final String LOG_TAG = TopTenActivityFragment.class.getSimpleName();

    private final String SAVE_KEY = "tracks";

    private ArrayList<TrackParcel> tracks;

    private TracksListAdapter mTopTenAdapter;
    private final SpotifyService spotifyService;

    public TopTenActivityFragment() {
        final SpotifyApi spotifyApi = new SpotifyApi();
        spotifyService = spotifyApi.getService();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (tracks != null && !tracks.isEmpty()) {
            outState.putParcelableArrayList(SAVE_KEY, tracks);
        }
        super.onSaveInstanceState(outState);
    }

    void showDialog(int position) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        PlayerDialog fragment = PlayerDialog.newInstance(tracks, position);
        fragment.show(fm, "dialog");
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_top_ten, container, false);

        mTopTenAdapter = new TracksListAdapter(getActivity(), new ArrayList<TrackParcel>());
        final ListView listView = (ListView) rootView.findViewById(R.id.listview_topten);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showDialog(position);
            }
        });

        listView.setAdapter(mTopTenAdapter);

        if (savedInstanceState == null || !savedInstanceState.containsKey(SAVE_KEY)) {
            final Bundle extras = getActivity().getIntent().getExtras();

            final String spotifyID = extras.getString("id");
            searchTrack(spotifyID);

            final String artistName = extras.getString("name");
            ((ActionBarActivity) getActivity()).getSupportActionBar().setSubtitle(artistName);
        } else {
            tracks = savedInstanceState.getParcelableArrayList(SAVE_KEY);
            mTopTenAdapter.addAll(tracks);
        }

        return rootView;
    }

    private void searchTrack(String spotifyID) {
        if (spotifyID == null || spotifyID.isEmpty()) {
            displayToast("Empty Spotify ID!");
        } else {
            spotifyService.getArtistTopTrack(spotifyID,
                    Collections.<String, Object>singletonMap("country", "us"),
                    new Callback<Tracks>() {
                        @Override
                        public void success(Tracks result, Response response) {
                            if (result.tracks.isEmpty()) {
                                displayToast("No Tracks found!");
                            } else {
                                final ArrayList<TrackParcel> parcels =
                                        TrackParcel.toParcelArrayList(result.tracks);
                                // update mSearchAdapter
                                mTopTenAdapter.clear();
                                mTopTenAdapter.addAll(parcels);
                                // store results locally
                                tracks = parcels;
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            Log.e(LOG_TAG, "error accessing Spotify API!", error);
                            displayToast("Error accessing Spotify API!");
                        }
                    });
        }
    }

    private void displayToast(final String message) {
        final Context context = getActivity();
        final Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private class TracksListAdapter extends ArrayAdapter<TrackParcel> {

        public TracksListAdapter(Context context, List<TrackParcel> tracks) {
            super(context, -1, tracks);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_topten, null);
            }

            TrackParcel track = getItem(position);

            if (track != null) {
                TextView albumTxt = (TextView) convertView.findViewById(R.id.item_top10_album);
                albumTxt.setText(track.albumName);
                TextView trackTxt = (TextView) convertView.findViewById(R.id.item_top10_track);
                trackTxt.setText(track.trackName);

                ImageView imageView = (ImageView) convertView.findViewById(R.id.item_top10_thumbnail);
                if (!track.thumbnailUrl.isEmpty()) {
                    Picasso.with(getContext()).load(track.thumbnailUrl)
                        .resize(200, 200).centerCrop().into(imageView);
                } else {
                    imageView.setImageResource(R.drawable.no_image_available);
                }
            }

            return convertView;
        }
    }

}
