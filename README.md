##ROICalculator(Option Premium Calculator)

ROI Calculator - Input the Premium price, Indices Name, Strike price and the Lot Size - It will provide you the exact investment with returns excluding platform taxes.


## 1. Install Java in Termux

```bash
pkg update
pkg install openjdk-21 git
```

Check it worked:

```bash
java -version
javac -version
```

## 2. Copy this folder onto your phone using git clone


```bash
git clone https://github.com/Hrushikesh07-ind/ROICalculator
```



## 3. Compile and run

```bash
cd ROICalculator/ROICalculator
javac Server.java
java Server 8080
```

You should see:

```
PriceCalculator server running.
Open http://localhost:8080 in your browser.
```

Leave this running in that Termux session (or run it with `nohup java Server 8080 &`
to keep it alive in the background).

## 4. Open the frontend

On the same phone, open any browser (Chrome, Firefox, etc.) and go to:

```
http://localhost:8080
```

Pick an index (NIFTY / BANKNIFTY / SENSEX), enter premium price, lot size,
and strike price, then tap **Calculate**. The page sends those values to
`Server.java`'s `/api/calculate` endpoint and shows quantity, investment
amount, and ROI — with the same math as your original console program:

```
quantity          = lotSize × multiplier      (nifty=65, banknifty=30, sensex=20)
investmentAmount  = price × quantity
roi               = (strikePrice × quantity) − investmentAmount
```

## Notes

- To use a different port: `java Server 9090`, then visit `http://localhost:9090`.
- To stop the server: `Ctrl+C` in the Termux session (or `kill` the process if backgrounded).
- No internet connection is needed once Java is installed — everything runs
  locally on your phone.
- If you want to edit the calculation logic, it's all in `Server.java`
  inside `investmentAmount()`, `roi()`, and `multiplierFor()` — same shape
  as your original static methods.
