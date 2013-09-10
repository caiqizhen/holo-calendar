package com.vdesmet.lib.calendar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.Object;
import java.util.Calendar;

public class CalendarView extends AbstractCalendarView implements View.OnClickListener {
    private static final int PADDING_COUNT = 14;

    public CalendarView(final Context context) {
        super(context);
        init();
    }

    public CalendarView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CalendarView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();

    }

    private void init() {
        setOrientation(VERTICAL);
        mIsViewInitialized = false;
        mFirstDayOfWeek = Calendar.MONDAY;
        mLastDayOfWeek = -1;

        // Update day width if we have usable values
        getViewTreeObserver().
        addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateDayWidth();
            }
        });
    }

    /**
     * Will be called by the TextView
     * This method will call the adapter's onDayClick method
     */
    @Override
    public void onClick(final View v) {
        // user clicked on a TextView
        if(v != null) {
            final long timeInMillis =  Long.parseLong(v.getTag().toString());
            if(mOnDayClickListener != null) {
                mOnDayClickListener.onDayClick(timeInMillis);
            }
        }
    }

    /**
     * Create the view
     * Initializes the headers, views for all visible days
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    protected void initView(final int width) {
        // if no custom lastDayOfWeek was set, change it to the day before the first day so we show all 7 days
        if(mLastDayOfWeek == -1) {
            mLastDayOfWeek = mFirstDayOfWeek - 1;
            if(mLastDayOfWeek <= 0) {
                mLastDayOfWeek = 7;
            }
        }

        // create the headers for the day of the week
        createHeaders();

        // setup the variables we'll need
        final Context context = getContext();
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final DayAdapter adapter = mDayAdapter;
        final Calendar currentDay = mCalendarFirstDay;
        final Calendar firstValidDay = mFirstValidDay;
        final Calendar lastValidDay = mLastValidDay;
        final int firstDayOfWeek = mFirstDayOfWeek;
        final int lastDayOfWeek = mLastDayOfWeek;
        final int currentMonth = mCurrentMonth;
        final int dayDisabledBackgroundColor = getResources().getColor(R.color.lib_calendar_day_background_disabled);
        final int dayDisabledTextColor = getResources().getColor(R.color.lib_calendar_day_textcolor_disabled);
        final Typeface typeface = mTypeface;

        ViewGroup weekLayout = (ViewGroup) inflater.inflate(R.layout.lib_calendar_week, this, false);

        // continue adding days until we've done the day at the end of the week(usually in the next month)
        while(currentDay.get(Calendar.MONTH) <= currentMonth || currentDay.get(Calendar.DAY_OF_WEEK) != lastDayOfWeek + 1) {

            // check if we need to add this day, if not, move to the next
            final int dayOfWeek = currentDay.get(Calendar.DAY_OF_WEEK);
            boolean moveToNext = false;
            if(lastDayOfWeek < firstDayOfWeek) {
                if((dayOfWeek < firstDayOfWeek && dayOfWeek > lastDayOfWeek) ||
                        (dayOfWeek > lastDayOfWeek && dayOfWeek < firstDayOfWeek)) {

                    // we don't want to have this dayOfWeek in our calendar, so move to the next one
                    moveToNext = true;
                }
            }
            else if(dayOfWeek < firstDayOfWeek || dayOfWeek > lastDayOfWeek) {
                // we don't want to have this dayOfWeek in our calendar, so move to the next one
                moveToNext = true;
            }
            if(moveToNext) {
                // move to the next day
                currentDay.add(Calendar.DAY_OF_WEEK, 1);
                continue;

            }
            // setup variables and layouts for this day
            final long timeInMillis = currentDay.getTimeInMillis();
            final ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.lib_calendar_day, this, false);
            final TextView dayTextView = (TextView) layout.findViewById(R.id.lib_calendar_day_text);
            final ViewGroup categories = (ViewGroup) layout.findViewById(R.id.lib_calendar_day_categories);

            // if set, use the custom Typeface
            if(typeface != null) {
                dayTextView.setTypeface(typeface);
            }

            // set the current day: 1-31
            final int dayOfMonth = currentDay.get(Calendar.DAY_OF_MONTH);
            dayTextView.setText(String.valueOf(dayOfMonth));

            // check if we need to disable the view
            //because it's in another month, or if it's before the first valid day, or after the last valid day
            // or if the adapter says it should be disabled
            if( !adapter.isDayEnabled(timeInMillis) ||
                    (currentDay.get(Calendar.MONTH) != currentMonth) ||
               (firstValidDay != null && currentDay.before(firstValidDay)) ||
               (lastValidDay != null && currentDay.after(lastValidDay))) {
                // change the appearance if it's disabled
                layout.setBackgroundColor(dayDisabledBackgroundColor);
                dayTextView.setTextColor(dayDisabledTextColor);
                // disable the views
                dayTextView.setEnabled(false);
                layout.setEnabled(false);
            }
            else {
                // allow the adapter to update the TextView
                // e.g. change TypeFace, font size, color based on the time
                adapter.updateTextView(dayTextView, timeInMillis);

                // create a new view for each category
                final int[] colors = adapter.getCategoryColors(timeInMillis);
                if(colors != null) {
                    for(final int color : colors) {
                        // inflate a new category
                        final View category = inflater.inflate(R.layout.lib_calendar_category, categories, false);

                        // set the background color to the color provided by the adapter
                        category.setBackgroundColor(color);

                        // add the view to the ViewGroup. Note that we can't do this while inflating
                        // because that will cause the view to match the size of his parent
                        categories.addView(category);
                    }
                }
            }

            // set TextView tag to the timeInMillis for the onClickListener
            dayTextView.setTag(timeInMillis);
            dayTextView.setOnClickListener(this);


            // add layout to view
            weekLayout.addView(layout);

            if(dayOfWeek == lastDayOfWeek) {
                // this is the last day in the week/row, add a new one
                addView(weekLayout);

                weekLayout = (ViewGroup) inflater.inflate(R.layout.lib_calendar_week, this, false);
            }

            // add 1 day
            currentDay.add(Calendar.DAY_OF_WEEK, 1);
        }

        if(weekLayout.getParent() == null) {
            addView(weekLayout);
        }

        // Update the day widths
        updateDayWidth();

        mIsViewInitialized = true;
    }

    /**
     * Create the headers for each (visible) day of the week
     * Starts at mFirstDayOfWeek, ends at mLastDayOfWeek
     */
    private void createHeaders() {
        // initialize variables
        final Context context = getContext();
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final Resources resources = context.getResources();
        final DayAdapter adapter = mDayAdapter;
        final int firstDayOfWeek = mFirstDayOfWeek;
        final int lastDayOfWeek = mLastDayOfWeek;
        final Typeface typeface = mTypeface;

        // inflate the ViewGroup where we'll put all the headers
        final ViewGroup headers = (ViewGroup) inflater.inflate(R.layout.lib_calendar_headers, this, false);
        int dayOfWeek = firstDayOfWeek;

        do {
            // initialize variables for this day
            final TextView header = (TextView) inflater.inflate(R.layout.lib_calendar_single_header, headers, false);
            final String nameOfDay = getNameForDay(dayOfWeek, resources);

            // if set, use the custom Typeface
            if(typeface != null) {
                header.setTypeface(typeface);
            }

            // allow adapter to update the TextView
            // e.g. change font, appearance, add click listener on all/some days
            adapter.updateHeaderTextView(header, dayOfWeek);

            // set the text
            header.setText(nameOfDay);

            // add TextView to ViewGroup
            headers.addView(header);

            // increment dayOfWeek, make sure it's a valid day
            dayOfWeek = dayOfWeek % 7;
            dayOfWeek++;

        } while(dayOfWeek != lastDayOfWeek + 1);

        // add the headers View
        addView(headers);
    }



    private int getAvailableDayWidth() {
        // Calculate the available width for a single day-item
        final int paddingSides = getResources()
                .getDimensionPixelSize(R.dimen.lib_calendar_day_padding_sides);
        final int screenWidth = getWidth();
        final int availableWidth = screenWidth - (paddingSides * PADDING_COUNT);
        final int daysInRow = getDaysInRow();
        final int widthPerTile = availableWidth / daysInRow;
        final int maxWidthPerTile =
                getResources().getDimensionPixelSize(R.dimen.lib_calendar_day_size);

        // The maximum size of a tile(e.g. a single day)
        // This is either R.dimen.lib_calendar_day_size or the width which fits the screen size
        final int tileSize = Math.min(widthPerTile, maxWidthPerTile);
        Log.d("CalendarView", "WidthPerTile: " + widthPerTile + ", maxWidthPerTile: " + maxWidthPerTile);
        Log.d("CalendarView", "screenwidth: " + screenWidth + ", daysInRow: " + daysInRow);
        return tileSize;
    }

    private void updateDayWidth() {
        final int dayWidth = getAvailableDayWidth();
        final int childCount = getChildCount();

        // Make sure the available day width is valid
        if(dayWidth > 0) {
            for(int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if(child instanceof  ViewGroup) {
                    final ViewGroup childViewGroup = (ViewGroup) child;
                    final int childItemCount = childViewGroup.getChildCount();

                    for(int index = 0; index < childItemCount; index++) {
                        final View dayView = childViewGroup.getChildAt(index);
                        if(dayView != null) {
                            final ViewGroup.LayoutParams params =
                                    dayView.getLayoutParams();

                            if(i == 0) {
                                // This is the dayOfWeek TextView, so we use wrap_content on the height
                                params.width = dayWidth;
                            } else {
                                // This is the layout for a single day which is a square
                                params.width = dayWidth;
                                params.height = dayWidth;
                            }
                            // Set the new LayoutParams
                            dayView.setLayoutParams(params);
                        }
                    }
                }
            }
            this.setVisibility(View.VISIBLE);
        } else {
            // We don't have a width available yet
            this.setVisibility(View.INVISIBLE);
        }
    }
}
