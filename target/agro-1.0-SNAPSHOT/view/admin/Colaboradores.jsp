<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%
  // Simulação de perfil. Troque para ADMIN, GESTOR ou VISITANTE
  String perfil = "ADMIN";
%>
<html>
<head>
    <title>Simulação - Painel Funcionários</title>

    <!-- Bootstrap 4 CSS -->
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">

    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

    <!-- Bootstrap 4 JS -->
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.bundle.min.js"></script>

    <!-- DataTables core -->
    <link rel="stylesheet" href="https://cdn.datatables.net/1.11.5/css/jquery.dataTables.min.css">
    <script src="https://cdn.datatables.net/1.11.5/js/jquery.dataTables.min.js"></script>

    <!-- DataTables Buttons -->
    <link rel="stylesheet" href="https://cdn.datatables.net/buttons/2.2.3/css/buttons.dataTables.min.css">
    <script src="https://cdn.datatables.net/buttons/2.2.3/js/dataTables.buttons.min.js"></script>
    <script src="https://cdn.datatables.net/buttons/2.2.3/js/buttons.html5.min.js"></script>
    <script src="https://cdn.datatables.net/buttons/2.2.3/js/buttons.print.min.js"></script>

    <!-- Dependências para export -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/pdfmake.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/pdfmake/0.2.7/vfs_fonts.js"></script>

    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
</head>
<body>
<div class="container-fluid mt-3">
    <h4>Painel de Funcionários (Simulação)</h4>

    <div class="form-row my-3">
        <div class="form-group col-md-3">
            <label for="filtroStatus">Filtrar por Status</label>
            <select id="filtroStatus" class="form-control">
                <option value="">Todos</option>
                <option value="ATIVO">ATIVO</option>
                <option value="INATIVO">INATIVO</option>
            </select>
        </div>
        <div class="form-group col-md-3">
            <label for="filtroCargo">Filtrar por Cargo</label>
            <select id="filtroCargo" class="form-control">
                <option value="">Todos</option>
            </select>
        </div>
    </div>

    <table id="tblFuncionarios" class="table table-striped table-hover w-100">
        <thead>
        <tr>
            <th>#</th>
            <th>Nome</th>
            <th>CPF</th>
            <th>Cargo</th>
            <th>E-mail</th>
            <th>Status</th>
            <th>Ações</th>
        </tr>
        </thead>
    </table>
</div>

<script>
/* =========================
   SIMULAÇÃO DE DADOS
========================= */
const funcionariosFake = [
  {id:1, nome:"Ana Souza", cpf:"123.456.789-00", cargo:"Analista", email:"ana@empresa.com", status:"ATIVO"},
  {id:2, nome:"Bruno Lima", cpf:"987.654.321-00", cargo:"Supervisor", email:"bruno@empresa.com", status:"INATIVO"},
  {id:3, nome:"Carlos Mendes", cpf:"111.222.333-44", cargo:"Gerente", email:"carlos@empresa.com", status:"ATIVO"},
  {id:4, nome:"Daniela Rocha", cpf:"555.666.777-88", cargo:"Analista", email:"daniela@empresa.com", status:"ATIVO"},
  {id:5, nome:"Eduardo Alves", cpf:"999.888.777-66", cargo:"Estagiário", email:"eduardo@empresa.com", status:"INATIVO"},
  {id:6, nome:"Fernanda Dias", cpf:"444.333.222-11", cargo:"Supervisor", email:"fernanda@empresa.com", status:"ATIVO"}
];

// Perfil vindo do servidor
const USER_ROLE = "<%= perfil %>";  // ADMIN, GESTOR, VISITANTE

/* =========================
   DATATABLE
========================= */
let dt;
const DT_COL = { ID:0, NOME:1, CPF:2, CARGO:3, EMAIL:4, STATUS:5, ACOES:6 };

function actionButtons(row) {
  const canEdit   = (USER_ROLE === "ADMIN" || USER_ROLE === "GESTOR");
  const canDelete = (USER_ROLE === "ADMIN");

  return `
    <div class="btn-group btn-group-sm">
      <button class="btn btn-outline-primary" ${canEdit ? "" : "disabled"}>
        <i class="fas fa-edit"></i>
      </button>
      <button class="btn btn-outline-danger" ${canDelete ? "" : "disabled"}>
        <i class="fas fa-trash-alt"></i>
      </button>
    </div>
  `;
}

function initTable() {
  dt = $("#tblFuncionarios").DataTable({
    data: funcionariosFake,
    columns: [
      { data: "id" },
      { data: "nome" },
      { data: "cpf" },
      { data: "cargo" },
      { data: "email" },
      { data: "status" },
      { data: null, render: (_,__,row) => actionButtons(row), orderable:false, searchable:false }
    ],
    language: { url: "https://cdn.datatables.net/plug-ins/1.11.5/i18n/pt-BR.json" },
    order: [[DT_COL.NOME, "asc"]],
    dom: 'Bfrtip',
    buttons: [
      { extend:'csvHtml5', text:'<i class="fas fa-file-csv"></i> CSV' },
      { extend:'pdfHtml5', text:'<i class="fas fa-file-pdf"></i> PDF', orientation:'landscape', pageSize:'A4' },
      { extend:'print', text:'<i class="fas fa-print"></i> Imprimir' }
    ]
  });

  preencherFiltroCargo(funcionariosFake);
}

/* =========================
   FILTROS
========================= */
function preencherFiltroCargo(lista) {
  const set = new Set(lista.map(x => x.cargo));
  const $sel = $("#filtroCargo");
  set.forEach(c => $sel.append('<option value="'+c+'">'+c+'</option>'));
}

function aplicarFiltros() {
  const st = $("#filtroStatus").val();
  const cg = $("#filtroCargo").val();
  dt.column(DT_COL.STATUS).search(st ? '^'+st+'$' : '', true, false);
  dt.column(DT_COL.CARGO).search(cg ? '^'+cg+'$' : '', true, false);
  dt.draw();
}

/* =========================
   INIT
========================= */
$(function(){
  initTable();
  $("#filtroStatus,#filtroCargo").on("change", aplicarFiltros);
});
</script>
</body>
</html>
