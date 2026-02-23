#property copyright "Custom for Android MT5 workflow"
#property version   "1.00"
#property indicator_chart_window
#property indicator_buffers 2
#property indicator_plots   2

#property indicator_label1  "BullishCross"
#property indicator_type1   DRAW_ARROW
#property indicator_color1  clrLime
#property indicator_width1  1

#property indicator_label2  "BearishCross"
#property indicator_type2   DRAW_ARROW
#property indicator_color2  clrTomato
#property indicator_width2  1

input int  FastEMAPeriod      = 20;
input int  SlowEMAPeriod      = 50;
input int  ArrowOffsetPoints  = 100;
input bool SignalOnClosedBar  = true;

double BullishBuffer[];
double BearishBuffer[];

int fastHandle = INVALID_HANDLE;
int slowHandle = INVALID_HANDLE;

int OnInit()
{
   if(FastEMAPeriod <= 0 || SlowEMAPeriod <= 0 || FastEMAPeriod >= SlowEMAPeriod)
   {
      Print("Invalid EMA settings: ensure 0 < FastEMAPeriod < SlowEMAPeriod.");
      return(INIT_PARAMETERS_INCORRECT);
   }

   SetIndexBuffer(0, BullishBuffer, INDICATOR_DATA);
   SetIndexBuffer(1, BearishBuffer, INDICATOR_DATA);
   ArraySetAsSeries(BullishBuffer, true);
   ArraySetAsSeries(BearishBuffer, true);

   PlotIndexSetInteger(0, PLOT_ARROW, 233); // Up arrow (Wingdings)
   PlotIndexSetInteger(1, PLOT_ARROW, 234); // Down arrow (Wingdings)
   PlotIndexSetDouble(0, PLOT_EMPTY_VALUE, EMPTY_VALUE);
   PlotIndexSetDouble(1, PLOT_EMPTY_VALUE, EMPTY_VALUE);

   IndicatorSetString(
      INDICATOR_SHORTNAME,
      "EMA Cross Arrows (" + IntegerToString(FastEMAPeriod) + "," + IntegerToString(SlowEMAPeriod) + ")"
   );

   fastHandle = iMA(_Symbol, _Period, FastEMAPeriod, 0, MODE_EMA, PRICE_CLOSE);
   slowHandle = iMA(_Symbol, _Period, SlowEMAPeriod, 0, MODE_EMA, PRICE_CLOSE);

   if(fastHandle == INVALID_HANDLE || slowHandle == INVALID_HANDLE)
   {
      Print("Failed to create EMA indicator handles.");
      return(INIT_FAILED);
   }

   return(INIT_SUCCEEDED);
}

void OnDeinit(const int reason)
{
   if(fastHandle != INVALID_HANDLE)
      IndicatorRelease(fastHandle);
   if(slowHandle != INVALID_HANDLE)
      IndicatorRelease(slowHandle);
}

int OnCalculate(
   const int rates_total,
   const int prev_calculated,
   const datetime &time[],
   const double &open[],
   const double &high[],
   const double &low[],
   const double &close[],
   const long &tick_volume[],
   const long &volume[],
   const int &spread[]
)
{
   if(rates_total < SlowEMAPeriod + 3)
      return(0);

   double fastEMA[];
   double slowEMA[];
   ArraySetAsSeries(fastEMA, true);
   ArraySetAsSeries(slowEMA, true);

   if(CopyBuffer(fastHandle, 0, 0, rates_total, fastEMA) <= 0)
      return(prev_calculated);
   if(CopyBuffer(slowHandle, 0, 0, rates_total, slowEMA) <= 0)
      return(prev_calculated);

   int start = 0;
   if(prev_calculated == 0)
   {
      ArrayInitialize(BullishBuffer, EMPTY_VALUE);
      ArrayInitialize(BearishBuffer, EMPTY_VALUE);
      start = rates_total - 2;
   }
   else
   {
      start = rates_total - prev_calculated + 1;
      if(start > rates_total - 2)
         start = rates_total - 2;
   }

   int newestSignalBar = SignalOnClosedBar ? 1 : 0;

   for(int i = start; i >= newestSignalBar; i--)
   {
      BullishBuffer[i] = EMPTY_VALUE;
      BearishBuffer[i] = EMPTY_VALUE;

      bool wasBelowOrEqual = (fastEMA[i + 1] <= slowEMA[i + 1]);
      bool isAbove         = (fastEMA[i] > slowEMA[i]);
      bool wasAboveOrEqual = (fastEMA[i + 1] >= slowEMA[i + 1]);
      bool isBelow         = (fastEMA[i] < slowEMA[i]);

      if(wasBelowOrEqual && isAbove)
         BullishBuffer[i] = low[i] - ArrowOffsetPoints * _Point;
      else if(wasAboveOrEqual && isBelow)
         BearishBuffer[i] = high[i] + ArrowOffsetPoints * _Point;
   }

   return(rates_total);
}
