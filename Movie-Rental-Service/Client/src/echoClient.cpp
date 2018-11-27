    #include <stdlib.h>
    #include <connectionHandler.h>
    #include <boost/thread.hpp>
    #include <thread>
    #include <iostream>
    #include <boost/thread.hpp>



    void task1(ConnectionHandler* connectionHandler) {
            const short bufsize = 1024;
            char buf[bufsize];

        while(1){
            std::cin.getline(buf, bufsize);
                    std::string line(buf);
            if (!(*connectionHandler).sendLine(line)) {
                break;
            }
        }
    }

    int main (int argc, char *argv[]) {
       if (argc < 3) {
          std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
          return -1;
     }
      std::string host = argv[1];
       short port = atoi(argv[2]);


        ConnectionHandler connectionHandler(host, port);
        if (!connectionHandler.connect()) {
            std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
            return 1;
        }
            boost::thread th1(task1, &connectionHandler); 
            while (1) {
                
          std::string answer;

        if (!connectionHandler.getLine(answer)) {
            break;
        }
        
        int len=answer.length();

        answer.resize(len-1);
                std::cout << answer << std::endl;
    
    }
    }
