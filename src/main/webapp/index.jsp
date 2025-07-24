<!DOCTYPE html>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <title>Info4Cloud - Dashboard</title>
    <!-- Bootstrap 4 CSS -->
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css">
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css">
    <!-- CSS customizado -->
    <link href="../CSS/estilo.css" rel="stylesheet">

</head>
<body>
    <header class="header">
        <%@ include file="../pagina/menu.jsp"%>
    </header>

    <div class="container-fluid content mt-2">
    <h2 class="mb-2 text-white border bg-success p-2 rounded shadow-custom">Dashboard</h2>
        <div class="row">
            <!-- Card 1 -->
            <div class="col-md-3 mb-4">
                <div class="card card-custom">
                    <div class="card-body">
                        <h5 class="card-title">Total de Despesas</h5>
                        <p class="card-text"><i class="fas fa-dollar-sign fa-2x"></i> R$ 12.345,67</p>
                    </div>
                </div>
            </div>
            <!-- Card 2 -->
            <div class="col-md-3 mb-4">
                <div class="card card-custom">
                    <div class="card-body">
                        <h5 class="card-title">Despesas Pendentes</h5>
                        <p class="card-text"><i class="fas fa-exclamation-circle fa-2x"></i> 15</p>
                    </div>
                </div>
            </div>
            <!-- Card 3 -->
            <div class="col-md-3 mb-4">
                <div class="card card-custom">
                    <div class="card-body">
                        <h5 class="card-title">Últimas Atualizações</h5>
                        <p class="card-text"><i class="fas fa-sync fa-2x"></i> Atualizado agora</p>
                    </div>
                </div>
            </div>
            <!-- Card 4 -->
            <div class="col-md-3 mb-4">
                <div class="card card-custom">
                    <div class="card-body">
                        <h5 class="card-title">Total de Custos</h5>
                        <p class="card-text"><i class="fas fa-chart-line fa-2x"></i> R$ 23.456,78</p>
                    </div>
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-md-8 mb-4">
                <!-- Tabela de Dados -->
                <div class="card card-custom">
                    <div class="card-header card-header-custom">
                        <h5 class="mb-0">Despesas Recentes</h5>
                    </div>
                    <div class="card-body">
                        <table class="table table-bordered table-custom">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Descrição</th>
                                    <th>Valor</th>
                                    <th>Data</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>1</td>
                                    <td>Compra de insumos</td>
                                    <td>R$ 1.200,00</td>
                                    <td>15/09/2024</td>
                                    <td><span class="badge badge-success">Pago</span></td>
                                </tr>
                                <tr>
                                    <td>2</td>
                                    <td>Manutenção de máquinas</td>
                                    <td>R$ 3.400,00</td>
                                    <td>14/09/2024</td>
                                    <td><span class="badge badge-warning">Pendente</span></td>
                                </tr>
                                <!-- Mais linhas de exemplo -->
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div class="col-md-4 mb-4">
                <!-- Gráfico ou Informação Adicional -->
                <div class="card card-custom">
                    <div class="card-header card-header-custom">
                        <h5 class="mb-0">Resumo</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="graficoResumo"></canvas>
                    </div>
                </div>
            </div>
        </div>
        
        <div class="row">
            <!-- Botões de Ação -->
            <div class="col-md-12 text-center">
                <button class="btn btn-custom m-2"><i class="fas fa-plus"></i> Adicionar Despesa</button>
                <button class="btn btn-custom m-2"><i class="fas fa-file-export"></i> Exportar Relatório</button>
            </div>
        </div>
    </div>

    <!-- Scripts -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.bundle.min.js"></script>
    <script>
        // Configuração do gráfico
        var ctx = document.getElementById('graficoResumo').getContext('2d');
        var graficoResumo = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: ['Pagas', 'Pendentes'],
                datasets: [{
                    data: [70, 30],
                    backgroundColor: ['#28a745', '#ffc107'],
                    borderColor: ['#ffffff', '#ffffff'],
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                legend: {
                    position: 'bottom'
                }
            }
        });
    </script>3 <footer>
        <%@ include file="../pagina/footer.jsp"%>
    </footer>
</body>
</html>
