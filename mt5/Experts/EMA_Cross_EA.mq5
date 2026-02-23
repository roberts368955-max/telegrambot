#property copyright "Custom for Android MT5 workflow"
#property version   "1.00"

#include <Trade/Trade.mqh>

input int    FastEMAPeriod        = 20;
input int    SlowEMAPeriod        = 50;
input double FixedLot             = 0.10;
input int    StopLossPoints       = 300;
input int    TakeProfitPoints     = 600;
input int    SlippagePoints       = 20;
input long   MagicNumber          = 20260223;
input bool   OnePositionPerSymbol = true;
input bool   CloseOppositeSignal  = true;

enum TradeSignal
{
   SIGNAL_NONE = 0,
   SIGNAL_BUY,
   SIGNAL_SELL
};

CTrade trade;
int fastHandle = INVALID_HANDLE;
int slowHandle = INVALID_HANDLE;
datetime lastBarTime = 0;

int OnInit()
{
   if(FastEMAPeriod <= 0 || SlowEMAPeriod <= 0 || FastEMAPeriod >= SlowEMAPeriod)
   {
      Print("Invalid EMA settings: ensure 0 < FastEMAPeriod < SlowEMAPeriod.");
      return(INIT_PARAMETERS_INCORRECT);
   }

   if(FixedLot <= 0.0)
   {
      Print("FixedLot must be greater than zero.");
      return(INIT_PARAMETERS_INCORRECT);
   }

   fastHandle = iMA(_Symbol, _Period, FastEMAPeriod, 0, MODE_EMA, PRICE_CLOSE);
   slowHandle = iMA(_Symbol, _Period, SlowEMAPeriod, 0, MODE_EMA, PRICE_CLOSE);

   if(fastHandle == INVALID_HANDLE || slowHandle == INVALID_HANDLE)
   {
      Print("Failed to create EMA indicator handles.");
      return(INIT_FAILED);
   }

   trade.SetExpertMagicNumber(MagicNumber);
   trade.SetDeviationInPoints(SlippagePoints);

   return(INIT_SUCCEEDED);
}

void OnDeinit(const int reason)
{
   if(fastHandle != INVALID_HANDLE)
      IndicatorRelease(fastHandle);
   if(slowHandle != INVALID_HANDLE)
      IndicatorRelease(slowHandle);
}

void OnTick()
{
   if(!IsNewBar())
      return;

   TradeSignal signal = GetSignal();
   if(signal == SIGNAL_NONE)
      return;

   if(OnePositionPerSymbol)
      ExecuteSinglePositionMode(signal);
   else
      OpenBySignal(signal);
}

bool IsNewBar()
{
   datetime currentBarTime = iTime(_Symbol, _Period, 0);
   if(currentBarTime == 0)
      return(false);

   if(currentBarTime == lastBarTime)
      return(false);

   lastBarTime = currentBarTime;
   return(true);
}

TradeSignal GetSignal()
{
   double fastEMA[];
   double slowEMA[];
   ArraySetAsSeries(fastEMA, true);
   ArraySetAsSeries(slowEMA, true);

   if(CopyBuffer(fastHandle, 0, 0, 3, fastEMA) < 3)
      return(SIGNAL_NONE);
   if(CopyBuffer(slowHandle, 0, 0, 3, slowEMA) < 3)
      return(SIGNAL_NONE);

   bool crossUp   = (fastEMA[2] <= slowEMA[2] && fastEMA[1] > slowEMA[1]);
   bool crossDown = (fastEMA[2] >= slowEMA[2] && fastEMA[1] < slowEMA[1]);

   if(crossUp)
      return(SIGNAL_BUY);
   if(crossDown)
      return(SIGNAL_SELL);

   return(SIGNAL_NONE);
}

void ExecuteSinglePositionMode(TradeSignal signal)
{
   ENUM_POSITION_TYPE currentType;
   bool hasPosition = GetPositionTypeForSymbol(currentType);

   if(!hasPosition)
   {
      OpenBySignal(signal);
      return;
   }

   if((signal == SIGNAL_BUY && currentType == POSITION_TYPE_BUY) ||
      (signal == SIGNAL_SELL && currentType == POSITION_TYPE_SELL))
   {
      return;
   }

   if(!CloseOppositeSignal)
      return;

   CloseOpenPositionsForSymbol();
   OpenBySignal(signal);
}

bool GetPositionTypeForSymbol(ENUM_POSITION_TYPE &positionType)
{
   int total = PositionsTotal();
   for(int i = total - 1; i >= 0; i--)
   {
      ulong ticket = PositionGetTicket(i);
      if(ticket == 0 || !PositionSelectByTicket(ticket))
         continue;

      string symbol = PositionGetString(POSITION_SYMBOL);
      long magic    = PositionGetInteger(POSITION_MAGIC);

      if(symbol == _Symbol && magic == MagicNumber)
      {
         positionType = (ENUM_POSITION_TYPE)PositionGetInteger(POSITION_TYPE);
         return(true);
      }
   }

   return(false);
}

void CloseOpenPositionsForSymbol()
{
   int total = PositionsTotal();
   for(int i = total - 1; i >= 0; i--)
   {
      ulong ticket = PositionGetTicket(i);
      if(ticket == 0 || !PositionSelectByTicket(ticket))
         continue;

      string symbol = PositionGetString(POSITION_SYMBOL);
      long magic    = PositionGetInteger(POSITION_MAGIC);
      if(symbol != _Symbol || magic != MagicNumber)
         continue;

      if(!trade.PositionClose(ticket))
      {
         Print(
            "Failed to close ticket ", ticket,
            " retcode=", trade.ResultRetcode(),
            " ", trade.ResultRetcodeDescription()
         );
      }
   }
}

void OpenBySignal(TradeSignal signal)
{
   double lot = NormalizeLot(FixedLot);
   if(lot <= 0.0)
   {
      Print("Lot size is not valid after normalization.");
      return;
   }

   bool result = false;
   if(signal == SIGNAL_BUY)
   {
      double price = SymbolInfoDouble(_Symbol, SYMBOL_ASK);
      double sl = (StopLossPoints > 0) ? NormalizeDouble(price - StopLossPoints * _Point, _Digits) : 0.0;
      double tp = (TakeProfitPoints > 0) ? NormalizeDouble(price + TakeProfitPoints * _Point, _Digits) : 0.0;
      result = trade.Buy(lot, _Symbol, price, sl, tp, "EMA cross buy");
   }
   else if(signal == SIGNAL_SELL)
   {
      double price = SymbolInfoDouble(_Symbol, SYMBOL_BID);
      double sl = (StopLossPoints > 0) ? NormalizeDouble(price + StopLossPoints * _Point, _Digits) : 0.0;
      double tp = (TakeProfitPoints > 0) ? NormalizeDouble(price - TakeProfitPoints * _Point, _Digits) : 0.0;
      result = trade.Sell(lot, _Symbol, price, sl, tp, "EMA cross sell");
   }

   if(!result)
   {
      Print(
         "Order send failed. retcode=", trade.ResultRetcode(),
         " ", trade.ResultRetcodeDescription()
      );
   }
}

double NormalizeLot(double lot)
{
   double minLot = SymbolInfoDouble(_Symbol, SYMBOL_VOLUME_MIN);
   double maxLot = SymbolInfoDouble(_Symbol, SYMBOL_VOLUME_MAX);
   double step   = SymbolInfoDouble(_Symbol, SYMBOL_VOLUME_STEP);

   if(step <= 0.0)
      step = 0.01;

   lot = MathMax(minLot, MathMin(maxLot, lot));
   lot = MathFloor(lot / step) * step;

   int lotDigits = (int)MathRound(-MathLog10(step));
   if(lotDigits < 0)
      lotDigits = 0;

   return(NormalizeDouble(lot, lotDigits));
}
