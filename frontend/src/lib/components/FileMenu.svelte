<script lang="ts">
  import { goto } from '$app/navigation';
  import { MODE_CODES } from '$lib/constants/schedule';
  import { modal } from '$lib/services/modal.service';
  import type { TimeInterval } from '$lib/types';
  import type { ProgramsListItem } from '$lib/types/analysis';
  import type { UserResponse } from '$lib/types/auth';
  import { mergeMsuIntervals } from '$lib/utils/interval/mergeMsuIntervals';
  import { onMount } from 'svelte';
  import { ScheduleApiService } from '../../features/services/api/scheduleApi.service';
  import { ProgramPreparerService } from '../../features/services/data/programPreparer.service';
  import { VpPreparerService } from '../../features/services/data/vpPreparer.service';
  
  let isOpen = $state(false);
  let isSubMenuOpen = $state(false);
  let menuRef = $state<HTMLDivElement | null>(null);
  let logoutForm: HTMLFormElement;
  let pendingLogout = false;

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
    numKa,
    onAfterSave,
    onNumRpSaved
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
    onAfterSave?: () => void;
    onNumRpSaved?: (numRp: number) => void;
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

        // В режиме анализа сохраняем все ПРЦА из списка
      if (isAnalysisMode && programsList.length > 0) {
          
          let savedCount = 0;
          let failedCount = 0;
          
          for (const program of programsList) {
              try {
                  
                  const redIntervals = createdPrograms.filter(p => p.timeInterval.hasConflict === true);

                  if (redIntervals.length > 0) {
                      modal.alert('Ошибка', 'Невозможно сохранить ПРЦА!\n\nОбнаружены конфликтующие интервалы (красные)\n\nПожалуйста, устраните конфликты вручную (удалите или переместите интервалы) и попробуйте снова.', 'error');
                      return;
                  }

                  const mergedCreatedPrograms = mergeMsuIntervals(createdPrograms);


                  // Подготавливаем данные для этой ПРЦА
                  const programRequest = ProgramPreparerService.prepareFullProgramData(
                      program.operatorData,
                      program.ppiAssignments,
                      mergedCreatedPrograms,
                      program.date,
                      "00:00",
                      'main',
                      program.numKa,
                      undefined
                  );

                  // Сохраняем
                  const result = await ScheduleApiService.saveProgram(programRequest);

                  // Сохранение ВПРЦА 
                  try {
                    const vpRequest = VpPreparerService.prepareVpData(
                      createdPrograms.filter(p => p.timeInterval.willBeSaved === true),
                      numKa || programRequest.mainData.numKa,
                      result?.numRp || 0,
                      programRequest.mainData.dateOn,
                      programRequest.mainData.dateOff
                      );

                      await ScheduleApiService.saveVp(vpRequest);
                  } catch (vpError) {
                      console.error(`Ошибка сохранения ВПРЦА для ${program.date}:`, vpError);
                  }

                  try {
                    await ScheduleApiService.generatePr01(
                      result.numRp,
                      numKa || programRequest.mainData.numKa
                    );
                  } catch (pr01Error) {
                    console.error
                  }

                  if (result?.numRp) {
                      program.numRp = result.numRp;
                      const index = programsList.findIndex(p => p.id === program.id);
                      if (index !== -1) {
                          programsList[index] = { ...program, numRp: result.numRp };
                      }
                  }
                  savedCount++;
                  
              } catch (error) {
                  console.error(`❌ Ошибка сохранения ПРЦА для ${program.date}:`, error);
                  failedCount++;
              }
          }
          onAfterSave?.();


          modal.alert('Успех', `Сохранение анализа завершено!\n\nУспешно: ${savedCount}\nОшибок: ${failedCount}`, 'success');
          
          return;
      }
      
      // 1. Логируем текущее состояние
      
      // 2. Проверяем данные
      if (!operatorData || !selectedProgramDate) {
          console.error("Недостаточно данных для сохранения!");
          modal.alert('Ошибка', 'Нет данных оператора или дата не выбрана', 'error');
          return;
      }
      
      if (intervals.length === 0 && (!operatorData.kvd_list?.length && !operatorData.tnp_list?.length && !operatorData.ts_list?.length)) {
          console.error("Нет интервалов для сохранения!");
          modal.alert('Ошибка', 'Нет интервалов для сохранения', 'error');
          return;
      }
      
      try {
          await updateAllConflicts();


          const redIntervals = createdPrograms.filter(p => p.timeInterval.hasConflict === true);

          if (redIntervals.length > 0) {
              modal.alert('Ошибка', 'Невозможно сохранить ПРЦА!\n\nОбнаружены конфликтующие интервалы (красные)\n\nПожалуйста, устраните конфликты вручную (удалите или переместите интервалы) и попробуйте снова.', 'error');
              return;
          }

          const mergedCreatedPrograms = mergeMsuIntervals(createdPrograms);

          // Подготавливаем данные программы
          const programRequest = ProgramPreparerService.prepareFullProgramData(
              operatorData,
              ppiAssignments,
              mergedCreatedPrograms,
              selectedProgramDate,
              "00:00",
              'main',
              numKa,
              undefined
          );
          
          // 3. Считаем для сохранения
          const kvdCount = programRequest.modes.filter(m => m.kodMode === MODE_CODES.KVD).length;
          const tnpCount = programRequest.modes.filter(m => m.kodMode === MODE_CODES.TNP).length;
          const tsCount = programRequest.modes.filter(m => m.kodMode === MODE_CODES.TS).length;
          const omiCount = programRequest.modes.filter(m => m.kodMode === MODE_CODES.OMI).length;
          const onaCount = programRequest.modes.filter(m => m.kodMode === MODE_CODES.ONA).length;
          const shootingCount = programRequest.modes.filter(m => m.kodMode === MODE_CODES.SHOOTING).length;
          
          // 4. Проверка времени интервалов
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
          
          const result = await ScheduleApiService.saveProgram(programRequest);

          // Сохранение ВПРЦА 
          try {
            const vpRequest = VpPreparerService.prepareVpData(
              createdPrograms.filter(p => p.timeInterval.willBeSaved === true),
              numKa || programRequest.mainData.numKa,
              result?.numRp || programRequest.mainData.numRp,
              programRequest.mainData.dateOn,
              programRequest.mainData.dateOff
              );

              await ScheduleApiService.saveVp(vpRequest);
          } catch (vpError) {
              console.error("Ошибка сохранения ВПРЦА:", vpError);
          }

          try {
            await ScheduleApiService.generatePr01(
              result.numRp,
              numKa || programRequest.mainData.numKa
            );
          } catch (pr01Error) {
            console.error
          }
          
          if (result?.numRp) {
              if (onNumRpSaved) {
                  onNumRpSaved(result.numRp);
              }
          }
          onAfterSave?.();

          modal.alert('Успех', `ПРЦА успешно сохранена!\n\nНомер РП: ${result?.numRp || programRequest.mainData.numRp}\nДата: ${selectedProgramDate}\nСохранено режимов: ${programRequest.modes.length}\n(КВД: ${kvdCount}, ТНП: ${tnpCount}, ТС: ${tsCount}, ОМИ: ${omiCount}, ОНА: ${onaCount}, Съемки: ${shootingCount})`, 'success');

      } catch (error) {
          console.error("❌ ОШИБКА ПРИ СОХРАНЕНИИ:", error);
          modal.alert('Ошибка', `Ошибка сохранения ПРЦА:\n${error.message}`, 'error');
      }
  }

  function handleExport() {
    modal.alert('Информация', 'Генерация отчета (в разработке)', 'info');
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
    modal.confirm(
      'Подтверждение',
      'Вы уверены, что хотите выйти?',
      async () => {
        // Отправляем запрос на выход
        const response = await fetch('/?/logout', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
        });
        
        if (response.ok) {
          // После успешного выхода — редирект на главную
          goto('/');
        } else {
          modal.alert('Ошибка', 'Не удалось выйти из системы', 'error');
        }
      },
      undefined,
      'warning'
    );
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
        <button onclick={handleLogout} class="menu-item logout">
          Выход
        </button>
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