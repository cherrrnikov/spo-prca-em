<script lang="ts">
  import { enhance } from '$app/forms';
  import { goto } from '$app/navigation';
  import type { TimeInterval } from '$lib/types';
  import type { ProgramsListItem } from '$lib/types/analysis';
  import type { UserResponse } from '$lib/types/auth';
  import { mergeMsuIntervals } from '$lib/utils/interval/mergeMsuIntervals';
  import { onMount } from 'svelte';
  import { ScheduleCreationService } from '../../features/services/scheduleCreation.service';
  
  let isOpen = $state(false);
  let isSubMenuOpen = $state(false);
  let menuRef = $state<HTMLDivElement | null>(null);

  let {
    userData,
    onOperatorCreate,
    onReferenceCreate,
    intervals = [],
    operatorData = null,
    ppiAssignments = [],
    selectedProgramDate = '',
    createdPrograms = [],
    onAnalysisClick,
    isAnalysisMode = false,
    isOperatorMode = false,
    updateAllConflicts,
    programsList = [],
    numKa
  } = $props<{
    userData: UserResponse | null;
    onOperatorCreate?: () => void;
    onReferenceCreate?: () => void;
    intervals?: TimeInterval[];
    operatorData?: any;
    ppiAssignments?: any[];
    selectedProgramDate?: string;
    createdPrograms?: any[];
    onAnalysisClick?: () => void;
    isAnalysisMode?: boolean;
    isOperatorMode?: boolean; 
    updateAllConflicts?: () => void;
    programsList?: ProgramsListItem[];
    numKa?: number;
  }>();

  function handleClickOutside(event: MouseEvent) {
    if (menuRef && !menuRef.contains(event.target as Node)) {
      isOpen = false;
      isSubMenuOpen = false;
    }
  }

  onMount(() => {
    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  });

  function handleCreateSchedule() {
    isSubMenuOpen = true;
  }

  function handleCreateByOperator() {
    if (onOperatorCreate) {
      onOperatorCreate();
    }

    isSubMenuOpen = false;
    isOpen = false;
  }

  function handleCreateByReference() {
    if (onReferenceCreate) {
      onReferenceCreate();
    }

    isSubMenuOpen = false;
    isOpen = false;
  }

  async function handleSave() {
      console.log("=== СОХРАНЕНИЕ ПРЦА ===");

        // В режиме анализа сохраняем все ПРЦА из списка
      if (isAnalysisMode && programsList.length > 0) {
          console.log(`Режим анализа: сохраняем ${programsList.length} ПРЦА`);
          
          let savedCount = 0;
          let failedCount = 0;
          
          for (const program of programsList) {
            const uniqueNumRp = Math.floor(Date.now() * 1000 + Math.random() * 1000);

              try {
                  console.log(`\n--- Сохранение ПРЦА для даты ${program.date} ---`);
                  console.log(`program.numKa для ${program.date}:`, program.numKa);
                  const mergedCreatedPrograms = mergeMsuIntervals(program.createdPrograms);

                  // Подготавливаем данные для этой ПРЦА
                  const programRequest = ScheduleCreationService.prepareFullProgramData(
                      program.operatorData,
                      program.ppiAssignments,
                      mergedCreatedPrograms,
                      program.date,
                      "00:00",
                      'main',
                      program.numKa,
                      uniqueNumRp
                  );
                  
                  console.log(`Количество режимов для ${program.date}: ${programRequest.modes.length}`);
                  console.log(`Режимы для ${program.date}:`, programRequest.modes.map(m => ({
                      kodMode: m.kodMode,
                      dateOn: m.dateOn,
                      dateOff: m.dateOff,
                      dlit: m.dlit
                  })));

                  // Сохраняем
                  const result = await ScheduleCreationService.saveProgram(programRequest);
                  console.log(`✅ ПРЦА для ${program.date} сохранена`);
                  savedCount++;
                  
              } catch (error) {
                  console.error(`❌ Ошибка сохранения ПРЦА для ${program.date}:`, error);
                  failedCount++;
              }
          }
          
          alert(`✅ Сохранение анализа завершено!\n\n` +
                `Успешно: ${savedCount}\n` +
                `Ошибок: ${failedCount}`);
          
          return;
      }
      
      // 1. Логируем текущее состояние
      console.log("Intervals (количество):", intervals.length);
      console.log("Operator Data:", operatorData);
      console.log("PPI Assignments:", ppiAssignments);
      console.log("Selected Date:", selectedProgramDate);
      console.log("Created Programs:", createdPrograms);
      
      // 2. Проверяем данные
      if (!operatorData || !selectedProgramDate) {
          console.error("Недостаточно данных для сохранения!");
          alert('Ошибка: Нет данных оператора или дата не выбрана');
          return;
      }
      
      if (intervals.length === 0 && (!operatorData.kvd_list?.length && !operatorData.tnp_list?.length && !operatorData.ts_list?.length)) {
          console.error("Нет интервалов для сохранения!");
          alert('Ошибка: Нет интервалов для сохранения');
          return;
      }
      
      try {
          const { ScheduleCreationService } = await import('../../features/services/scheduleCreation.service');
          
          console.log("=== ПОДГОТОВКА ДАННЫХ ДЛЯ СОХРАНЕНИЯ ===");
          await updateAllConflicts();

          const mergedCreatedPrograms = mergeMsuIntervals(createdPrograms);
          console.log(`После объединения: ${mergedCreatedPrograms.length} интервалов (было ${createdPrograms.length})`);

          // Подготавливаем данные программы
          const programRequest = ScheduleCreationService.prepareFullProgramData(
              operatorData,
              ppiAssignments,
              mergedCreatedPrograms,
              selectedProgramDate,
              "00:00",
              'main',
              numKa,
              undefined
          );
          
          // 3. ЛОГИРУЕМ ВСЕ ДАННЫЕ, КОТОРЫЕ БУДУТ ОТПРАВЛЕНЫ
          console.log("=== СОЗДАННЫЙ ЗАПРОС НА СОХРАНЕНИЕ ===");
          console.log("Полный запрос (JSON):", JSON.stringify(programRequest, null, 2));
          
          console.log("\n=== ДЕТАЛЬНЫЙ АНАЛИЗ ===");
          console.log("Основные данные программы:");
          console.log("- Номер ПРЦА:", programRequest.mainData.numRp);
          console.log("- Номер КА:", programRequest.mainData.numKa);
          console.log("- Дата начала:", programRequest.mainData.dateOn);
          console.log("- Дата окончания:", programRequest.mainData.dateOff);
          console.log("- Тип ПРЦА:", programRequest.mainData.typeRp);
          
          console.log("\nРежимы работы (" + programRequest.modes.length + " шт.):");
          programRequest.modes.forEach((mode, index) => {
              console.log(`\n--- Режим ${index + 1} ---`);
              console.log("- Код режима:", mode.kodMode);
              console.log("- Дата начала:", mode.dateOn);
              console.log("- Дата окончания:", mode.dateOff);
              console.log("- Номер ППИ:", mode.numPpi);
              console.log("- Длительность:", mode.dlit, "сек");
              console.log("- Заказчик:", mode.zakazchik || "не указан");
              
              if (mode.kvdData) {
                  console.log("- Тип: КВД");
                  console.log("  МСУ:", mode.kvdData.prMsu === 0 ? "МСУ-1" : "МСУ-2");
                  console.log("  БССД:", mode.kvdData.prBssd === 0 ? "БССД1" : "БССД2");
                  console.log("  ЗГ:", "ЗГ" + (mode.kvdData.prZg + 1));
              }
              
              if (mode.tsData) {
                  console.log("- Тип: ТС");
                  console.log("  Тип съемки:", mode.tsData.tip === 1 ? "штатная" : "учащенная");
                  console.log("  Режим:", mode.tsData.reg);
                  console.log("  МСУ1:", mode.tsData.prMsu1 ? "задействован" : "не задействован");
                  console.log("  МСУ2:", mode.tsData.prMsu2 ? "задействован" : "не задействован");
                  console.log("  БССД:", mode.tsData.prBssd ? "включен" : "выключен");
                  console.log("  ЗГ:", "ЗГ" + (mode.tsData.prZg + 1));
              }
              
              if (mode.tnpData) {
                  console.log("- Тип: ТНП");
                  console.log("  Длительность:", mode.dlit, "сек");
              }
              
              if (mode.omiData) {
                  console.log("- Тип: ОМИ");
                  console.log("  Номер ОМИ:", mode.omiData.numOmi);
                  console.log("  Тип ОМИ:", mode.omiData.typeOmi);
              }
              
              if (mode.onaData) {
                  console.log("- Тип: Юстировка ОНА");
                  console.log("  Номер антенны:", mode.onaData.nOna);
              }
          });
          
          console.log("\n=== СВОДКА ===");
          const kvdCount = programRequest.modes.filter(m => m.kodMode === 7).length;
          const tnpCount = programRequest.modes.filter(m => m.kodMode === 4).length;
          const tsCount = programRequest.modes.filter(m => m.kodMode === 8).length;
          const omiCount = programRequest.modes.filter(m => m.kodMode === 2).length;
          const onaCount = programRequest.modes.filter(m => m.kodMode === 6).length;
          const shootingCount = programRequest.modes.filter(m => m.kodMode === 1).length;
          
          console.log(`Всего режимов: ${programRequest.modes.length}`);
          console.log(`КВД: ${kvdCount}`);
          console.log(`ТНП: ${tnpCount}`);
          console.log(`ТС: ${tsCount}`);
          console.log(`ОМИ: ${omiCount}`);
          console.log(`Юст.ОНА: ${onaCount}`);
          console.log(`Съемки: ${shootingCount}`);
          
          // 4. Проверка времени интервалов
          console.log("\n=== ПРОВЕРКА ВРЕМЕНИ ИНТЕРВАЛОВ ===");
          let hasCrossDayIntervals = false;
          programRequest.modes.forEach((mode, index) => {
              const startDate = new Date(mode.dateOn);
              const endDate = new Date(mode.dateOff);
              
              if (startDate.getDate() !== endDate.getDate()) {
                  console.warn(`Режим ${index + 1} пересекает границу суток!`);
                  console.warn(`  Начало: ${mode.dateOn}`);
                  console.warn(`  Конец: ${mode.dateOff}`);
                  hasCrossDayIntervals = true;
              }
          });
          
          if (hasCrossDayIntervals) {
              console.warn("ВНИМАНИЕ: Найдены интервалы, пересекающие границу суток!");
          }
          
          // 5. РЕАЛЬНОЕ СОХРАНЕНИЕ
          console.log("\n=== ОТПРАВКА ЗАПРОСА НА БЭКЕНД ===");
          
          const result = await ScheduleCreationService.saveProgram(programRequest);
          
          console.log("=== РЕЗУЛЬТАТ СОХРАНЕНИЯ ===");
          console.log("Успешно! Ответ сервера:", result);
          
          alert(`✅ ПРЦА успешно сохранена!\n\n` +
                `Номер ПРЦА: ${programRequest.mainData.numRp}\n` +
                `Дата: ${selectedProgramDate}\n` +
                `Сохранено режимов: ${programRequest.modes.length}\n` +
                `(КВД: ${kvdCount}, ТНП: ${tnpCount}, ТС: ${tsCount} - временно пропускаются, ОМИ: ${omiCount}, ОНА: ${onaCount})`);
          

      } catch (error) {
          console.error("❌ ОШИБКА ПРИ СОХРАНЕНИИ:", error);
          alert(`❌ Ошибка сохранения ПРЦА:\n${error.message}\n\nПодробности в консоли (F12)`);
      }
  }

  function handleExport() {
    alert('Генерация отчета');
  }

  function handleAnalysis() {
    if (onAnalysisClick) {
        onAnalysisClick();
    }
    isOpen = false;
  }

  function handleArchive() {
    goto('/archive');
  }

  function handleLogout() {
    if (confirm('Вы уверены, что хотите выйти?')) {
      goto('/');
    }
  }

  function handleAdmin() {
    goto('/admin');
  }

  function toggleMenu(event: MouseEvent) {
    event.stopPropagation();
    isOpen = !isOpen;
    if (!isOpen) {
      isSubMenuOpen = false;
    }
  }
</script>

<div class="file-menu" bind:this={menuRef}>
  <button class="menu-button" onclick={toggleMenu}>
    Действия
  </button>

  {#if isOpen}
    <div class="dropdown">
      <div class="dropdown-content">
        <div onclick={handleCreateSchedule} class="menu-item with-submenu">
          Создать ПРЦА
          {#if isSubMenuOpen}
            <div class="submenu">
              <button onclick="{handleCreateByOperator}" class="submenu-item">
                По данным оператора
              </button>
              <button onclick="{handleCreateByReference}" class="submenu-item">
                По опорной ПРЦА
              </button>
            </div>
          {/if}
        </div>
        <button onclick={handleSave} class="menu-item">
          Сохранить ПРЦА
        </button>
        <hr />
        <button onclick={handleExport} class="menu-item">
          Отчет
        </button>
        {#if isOperatorMode && intervals && intervals.length > 0 && !isAnalysisMode}
          <button onclick={handleAnalysis} class="menu-item analysis">
            Анализ
          </button>
        {/if}
        <button onclick={handleArchive} class="menu-item">
          Просмотр архива
        </button>
        <hr />
                
        <div class="user-info">
            {#if userData}
                <div class="user-details">
                    <div class="user-name">
                        {userData.firstName} {userData.lastName}
                    </div>
                    <div class="user-meta">
                        <span class="username">@{userData.username}</span>
                        {#if userData.roles && userData.roles.length > 0}
                            <span class="user-role">• {userData.roles[0]}</span>
                        {/if}
                    </div>
                </div>
            {:else}
                <div class="user-details">
                    <div class="user-name">
                        Не авторизован
                    </div>
                </div>
            {/if}
        </div>
        {#if userData?.roles?.includes('ADMIN')}
            <button onclick={handleAdmin} type="submit" class="menu-item">Панель админа</button>
        {/if}
        <form method="POST" action="/?/logout" use:enhance>
            <button onclick={handleLogout} type="submit" class="menu-item logout">
            Выход
            </button>
        </form>
      </div>
    </div>
  {/if}
</div>

<style>
  .file-menu {
    position: relative;
    z-index: 1000;
  }

  .menu-button {
    background: #4299e1;
    color: white;
    border: none;
    padding: 0.75rem 1.5rem;
    border-radius: 6px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 500;
    transition: background 0.2s;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .menu-button:hover {
    background: #3182ce;
  }

  .dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    margin-top: 0.5rem;
    z-index: 1001;
  }

  .dropdown-content {
    background: white;
    border-radius: 8px;
    box-shadow: 0 10px 25px rgba(0,0,0,0.1);
    min-width: 220px;
    overflow: visible;
    border: 1px solid #e2e8f0;
    position: relative;
  }

  .menu-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    width: 100%;
    padding: 0.875rem 1rem;
    border: none;
    background: none;
    text-align: left;
    cursor: pointer;
    font-size: 0.9rem;
    color: #2d3748;
    transition: background 0.2s;
    position: relative;
  }

  .menu-item.with-submenu {
    position: relative;
  }

  .menu-item:hover {
    background: #f7fafc;
  }

  .menu-item.logout {
    color: #e53e3e;
  }

  hr {
    margin: 0;
    border: none;
    border-top: 1px solid #e2e8f0;
  }
  
  .arrow-icon {
    opacity: 0.6;
    transition: transform 0.2s;
  }
  
  .menu-item.with-submenu:hover .arrow-icon {
    opacity: 1;
  }
  
  .submenu {
    position: absolute;
    top: 0;
    left: 100%;
    margin-left: 2px;
    background: white;
    border-radius: 8px;
    box-shadow: 0 10px 25px rgba(0,0,0,0.1);
    min-width: 220px;
    border: 1px solid #e2e8f0;
    z-index: 1002;
  }
  
  .submenu-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    padding: 0.875rem 1rem;
    border: none;
    background: none;
    text-align: left;
    cursor: pointer;
    font-size: 0.9rem;
    color: #2d3748;
    transition: background 0.2s;
  }
  
  .submenu-item:hover {
    background: #f7fafc;
  }
  
  .user-info {
      display: flex;
      flex-direction: column;
      padding: 1rem;
      gap: 12px;
  }
  
  .user-details {
      text-align: left;
  }
  
  .user-name {
      font-size: 1rem;
      font-weight: 600;
      color: #2d3748;
      line-height: 1.2;
  }
  
  .user-meta {
      font-size: 0.85rem;
      color: #718096;
      display: flex;
      align-items: center;
      gap: 6px;
      line-height: 1.2;
  }
  
  .username {
      font-weight: 500;
  }
  
  .user-role {
      font-style: italic;
  }
</style>