//TCC desenvolvido com equipamentos fornecidos pelo edital santos dummond da FAPEMIG

char val; // variable to receive data from the serial port
int r_motor_n = 5; //PWM control Right Motor - 
int r_motor_p = 6; //PWM control Right Motor + 
int l_motor_p = 10; //PWM control Left Motor + 
int l_motor_n = 11; //PWM control Left Motor -

char lastCommand;       // Variable that contains the last command requested by user
char bLastCommand;      // Variable that contains the before of last command requested by user
char commandReceived;   // Variable that contains the received command to identify it
String pwm = "";        // Variable with current PWM
float lastPWM = 255;    // Variable with last PWM
boolean hasRamp = false;// Variable to indicate if movement has automatic ramp or not
int limit = 150;              // Specifies the limit of the car to leave the inertia
float inc = 0.0025;     // Indicates an increment which makes the movement smooth

int state;  // The current evaluation's state of the data received, it can be:
            //0 - no command; 1 - & received; 2 - W received; 3 - R received;
            //4 - F, B, R or L received

// This function receives the data from Bluetooth's adaptor and evaluates it
// It verifies the protocol of communication:
// &WF = Command to go foward
// &WB = Command to go backwards
// &WR = Command to turn right
// &WL = Command to turn left
// &RF000 = Command to go foward with a value to PWM from the accelerometer
// &RB000 = Command to go backwards with a value to PWM from the accelerometer

void evaluateCommand(){

  static int count;
  
  if( Serial.available() ) {     // if data is available to read
    val = Serial.read();         // read it and store it in 'val'
  } else {
    return;
  }
  
  switch(state){
    
    case 0: //No command previously    
          if(val == '&') {
            state = 1; 
          }
          break;
    
    case 1: //Command already started with &    
          if (val == 'W') { //Verifies if it hasn't the accelerometer's value
            state = 2;
          } else if (val == 'R') {
            state = 3;
          }
          break;         
    
    case 2: //Command without ramp: automatic ramp    
          if (val == 'F'){
            doFoward(false, -1);
          } else if (val == 'B') {
            doBackwards(false, -1);          
          } else if (val == 'R') {
            doRight();
          } else if (val == 'L') {
            doLeft();
          } else if (val == 'S') {
            doStop();            
          }
          state = 0;
          break;
          
    case 3: //Command with ramp: PWM uses accelerometer    
          if ((val == 'F') || (val == 'B')){
            commandReceived = val;
            state = 4;
            count = 0;
          } 
          break;
         
    case 4: //Command with ramp: getting accelerometer's value    
          if (count < 3){ //gets three accelerometer's caracters 
            pwm = pwm + String(val);
            count++;
          }          
          if (count == 3){ 
            //with the accelerometer's value, it can go foward or backwards            
            if (commandReceived == 'F'){
              doFoward(true, pwm.toInt()); 
            } else if (commandReceived == 'B'){
              doBackwards(true, pwm.toInt());
            }
            state = 0;
            count = 0;
            pwm = "";
          }
          break;
  }  
}

//This function verifies which command was requested
//Updates the movement of the car
void updateCommand(){  
  if (hasRamp){ //Has it automatic ramp?    
    if (lastCommand == 'F'){
      //If lastCommand was foward, it has to decrement the lastPWM 
      //for the difference of potencial rise and go even further      
      if (lastPWM > limit){      
        lastPWM = lastPWM - inc;        
        analogWrite(r_motor_p, (int)lastPWM);
        analogWrite(l_motor_p, (int)lastPWM);      
      } else {
        //If the lastPWM reaches the limit, the automatic ramp is no longer necessary
        hasRamp = false;        
      }      
    } else if (lastCommand == 'B'){
      //If lastCommand was backwards, it has to decrement the lastPWM 
      //for the difference of potencial rise and go even further      
      if (lastPWM > limit){      
        lastPWM = lastPWM - inc;
        analogWrite(r_motor_n, (int)lastPWM);
        analogWrite(l_motor_n, (int)lastPWM);      
      } else {        
        hasRamp = false;        
      }      
    } else if (lastCommand == 'P'){ 
      //Going foward and has to stop
      //It has to make a smooth stop, so it makes a break ramp      
      if (lastPWM < 255){        
        lastPWM = lastPWM + inc;
        analogWrite(r_motor_p, (int)lastPWM);
        analogWrite(l_motor_p, (int)lastPWM);        
      } else {        
        hasRamp = false;        
      }      
    } else if (lastCommand == 'S'){ 
      //Going backwards and has to stop
      //It has to make a smooth stop, so it makes a break ramp      
      if (lastPWM < 255){
        lastPWM = lastPWM + inc;
        analogWrite(r_motor_n, (int)lastPWM);
        analogWrite(l_motor_n, (int)lastPWM);        
      } else {        
        hasRamp = false;        
      }       
    } 
  }
}

//This function is called when the command received is to go foward
//So, it evaluates how to go foward: with or without automatic ramp, and does it
void doFoward(boolean ramp, int val){ 
  
  if(lastCommand == 'F') return; 
  
  digitalWrite(r_motor_n, HIGH);
  digitalWrite(l_motor_n, HIGH);
  
  if( lastCommand == 'R' || lastCommand == 'L'){
    if(lastPWM < 195){
      ramp = true;
      val = lastPWM;
    }else{
      ramp = false;
    }
  }  
  if (ramp) {      
    analogWrite(r_motor_p, val);
    analogWrite(l_motor_p, val);
    lastPWM = val;
    hasRamp = false;    
  } else {    
    //Fist value of lastPWM
    lastPWM = 255;
    hasRamp = true;
    analogWrite(r_motor_p, 255);
    analogWrite(l_motor_p, 255);    
  }
  bLastCommand = lastCommand;  
  lastCommand = 'F';
}

//This function is called when the command received is to go backwards
//So, it evaluates how to go backwards: with or without automatic ramp, and does it
void doBackwards(boolean ramp, int val){
  
  if(lastCommand == 'B') return;
  
  digitalWrite(r_motor_p, HIGH);
  digitalWrite(l_motor_p, HIGH);  
  if( lastCommand == 'R' || lastCommand == 'L'){
    if(lastPWM < 195){
      ramp = true;
      val = lastPWM;
    }else{
      ramp = false;
    }
  }  
  if (ramp) {      
    analogWrite(r_motor_n, val);
    analogWrite(l_motor_n, val);
    lastPWM = val;
    hasRamp = false;    
  } else {    
    //Fist value of lastPWM
    lastPWM = 255;
    hasRamp = true;
    analogWrite(r_motor_n, 255);
    analogWrite(l_motor_n, 255);  
  }
  bLastCommand = lastCommand;  
  lastCommand = 'B';
}

//This function is called when the command received is to turn right
//It verifies the last command to know which motor has to change the speed
void doRight(){  
  if (lastPWM > 195) {      
    digitalWrite(l_motor_n, HIGH);
    digitalWrite(r_motor_p, HIGH);
    analogWrite(l_motor_p, 195);
    analogWrite(r_motor_n, 195);
    lastPWM = 196;
    hasRamp = false;    
  } else {    
    if (lastCommand == 'F'){       
      if (lastPWM == 0) lastPWM = 10;
      analogWrite(l_motor_p, lastPWM);
      analogWrite(r_motor_p, lastPWM*1.3);      
    } else if (lastCommand == 'B') {      
      if (lastPWM == 0) lastPWM = 10;
      analogWrite(l_motor_n, lastPWM*1.3);
      analogWrite(r_motor_n, lastPWM);      
    }    
    hasRamp = true;
  }  
  bLastCommand = lastCommand;  
  lastCommand = 'R';
}

//This function is called when the command received is to turn left
//It verifies the last command to know which motor has to change the speed
void doLeft(){    
  if (lastPWM > 195) {    
    digitalWrite(l_motor_p, HIGH);
    digitalWrite(r_motor_n, HIGH);
    analogWrite(l_motor_n, 195);
    analogWrite(r_motor_p, 195);
    lastPWM = 196;
    hasRamp = false;    
  } else {
    if (lastCommand == 'F'){      
      if (lastPWM == 0) lastPWM = 10;
      analogWrite(l_motor_p, lastPWM*1.3);
      analogWrite(r_motor_p, lastPWM);
    } else if (lastCommand == 'B') {      
      if (lastPWM == 0) lastPWM = 10;
      analogWrite(l_motor_n, lastPWM);
      analogWrite(r_motor_n, lastPWM*1.3);      
    }
    hasRamp = true;    
  }  
  bLastCommand = lastCommand;    
  lastCommand = 'L';
}

//This function is called when the command received is to stop the car
//It verifies the last command to know if it has to make a automatic ramp or not
void doStop(){
  if (lastCommand == 'R' || lastCommand == 'L'){    
    if(bLastCommand != 'F' && bLastCommand != 'B'){
      digitalWrite(r_motor_n, LOW); 
      digitalWrite(r_motor_p, LOW); 
      digitalWrite(l_motor_p, LOW); 
      digitalWrite(l_motor_n, LOW);
      lastPWM = 255;
    }else if(bLastCommand == 'F'){
      hasRamp = true;
      lastCommand = 'P';
    }else{
      hasRamp = true;
      lastCommand = 'S';
    }    
  } else if (lastCommand == 'F'){
    hasRamp = true;
    lastCommand = 'P';
  } else if (lastCommand == 'B'){
    hasRamp = true;
    lastCommand = 'S';
  }  
}

//This function makes the setup of pins from the arduino
void setup() {  
  pinMode(r_motor_n, OUTPUT); //Set control pins to be outputs 
  pinMode(r_motor_p, OUTPUT); 
  pinMode(l_motor_p, OUTPUT); 
  pinMode(l_motor_n, OUTPUT);   
  digitalWrite(r_motor_n, LOW); //set both motors off for start-up 
  digitalWrite(r_motor_p, LOW); 
  digitalWrite(l_motor_p, LOW); 
  digitalWrite(l_motor_n, LOW);  
  Serial.begin(9600);       // start serial communication at 9600bps
}

//This function is the main part of the program: 
//it has the loop of evaluating the command received and doing something about it
void loop() {
  evaluateCommand();
  updateCommand();
  
} 
