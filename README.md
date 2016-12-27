# Visualizing-News-Reporting
Develop an application to visualize news articles over time.

Steps to install this application on Unix box.
---------------For installing Java 8 on 64-bit Systems --------------
$ cd /opt/java
 wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jdk-8u45-linux-x64.tar.gz"
$ tar -zxvf jdk-8u45-linux-x64.tar.gz
$ cd jdk1.8.0_45/

$ update-alternatives --install /usr/bin/java java /opt/java/jdk1.8.0_45/bin/java 100  
$ update-alternatives --config java

$ update-alternatives --install /usr/bin/javac javac /opt/java/jdk1.8.0_45/bin/javac 100
$ update-alternatives --config javac

$ update-alternatives --install /usr/bin/jar jar /opt/java/jdk1.8.0_45/bin/jar 100
$ update-alternatives --config jar

$ export JAVA_HOME=/opt/java/jdk1.8.0_45/	
$ export JRE_HOME=/opt/java/jdk1.8.0._45/jre 	
$ export PATH=$PATH:/opt/java/jdk1.8.0_45/bin:/opt/java/jdk1.8.0_45/jre/bin

---------------For installing MongoDB --------------

$ mkdir mongodb
$ cd mongodb/
$ curl -O http://downloads.mongodb.org/linux/mongodb-linux-x86_64-2.6.3.tgz

$ tar xvf mongodb-linux-x86_64-2.6.3.tgz 

$ mv mongodb-linux-x86_64-2.6.3 mongodb
$ cd mongodb
$ echo $PATH
$ export PATH=$PATH:/home/rahulkhanna/mongodb/mongodb/bin
$ mkdir data
$ cd bin

$ ./mongod --dbpath /home/rahulkhanna/mongodb/mongodb/data &

-------------Deploying Code------------------

1) Import the code in intelliJ and build the project as maven project.
2) Upon building, it will create a jar file which needs to be placed on the unix box.

------------Building collection in MongoDB---------------------

1)  Download the data from the below link and rename it to data.csv

https://dl.dropboxusercontent.com/u/428478238/research/backtest_students_sample.csv.gz 

2) Place data.csv in the root directory of the application.
3) Run com.newsvisualizer.visualization.predataoperations.DataEnricher.java which will create the data and upload it in mongodb.

---------------Run Application-----------------

1) Run com.newsvisualizer.visualization.bootstrap.Application.java to run the application.
2) Run simple python server in the directory where index.html is placed which is the root directory of the application.

--------------Demo Video Link-----------------

https://www.youtube.com/watch?v=0ef_En0JIW8&feature=youtu.be 
