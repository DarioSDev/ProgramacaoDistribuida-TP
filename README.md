Diretoria:

Bash

java -cp target/ProgramacaoDistribuida-TP-1.0-SNAPSHOT.jar pt.isec.pd.directory.Main 5001

Servidor:

Bash

java -cp target/ProgramacaoDistribuida-TP-1.0-SNAPSHOT.jar pt.isec.pd.server.Main 127.0.0.1:5001 230.30.30.30 ./db_storage

Cliente:

Bash

java -cp target/ProgramacaoDistribuida-TP-1.0-SNAPSHOT.jar pt.isec.pd.client.Main 127.0.0.1 5001
